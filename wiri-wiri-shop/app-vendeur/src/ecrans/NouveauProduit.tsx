import { Ionicons } from '@expo/vector-icons';
import {
  AudioModule,
  RecordingPresets,
  setAudioModeAsync,
  useAudioRecorder,
  useAudioRecorderState,
} from 'expo-audio';
import * as ImagePicker from 'expo-image-picker';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Animated, Easing, Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { api, ErreurApi } from '../api';
import { couleurs, espace, ombre, rayon, typo } from '../theme';
import { Bouton, Carte, Champ, Ecran, EnTete, Message, Pictogramme, vibrer } from '../ui';

/**
 * Création d'une fiche en ~5 secondes :
 * 1. photo de l'article ;
 * 2. le vendeur décrit l'article en wolof (appui maintenu) → l'IA pré-remplit nom / prix / taille ;
 * 3. le vendeur vérifie et publie.
 */
export default function NouveauProduit({ vocalAutorise, onDemanderConsentement, onTermine }: {
  vocalAutorise: boolean;
  onDemanderConsentement: () => void;
  onTermine: () => void;
}) {
  const enregistreur = useAudioRecorder(RecordingPresets.HIGH_QUALITY); // AAC → .m4a
  const etatEnregistreur = useAudioRecorderState(enregistreur);

  const [photo, setPhoto] = useState<ImagePicker.ImagePickerAsset | null>(null);
  const [nom, setNom] = useState('');
  const [prix, setPrix] = useState('');
  const [taille, setTaille] = useState('');
  const [transcription, setTranscription] = useState<string | null>(null);
  const [attente, setAttente] = useState<null | 'analyse' | 'publication'>(null);
  const [message, setMessage] = useState<{ type: 'erreur' | 'info'; texte: string } | null>(null);

  const prendrePhoto = async () => {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) return setMessage({ type: 'erreur', texte: 'Autorisez l\'appareil photo dans les réglages.' });
    const resultat = await ImagePicker.launchCameraAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.6, // image légère pour les réseaux 3G
      exif: false, // pas de métadonnées (localisation) envoyées
    });
    if (!resultat.canceled) setPhoto(resultat.assets[0]);
  };

  // État réel du micro, indépendant du rafraîchissement de l'interface (évite les courses entre appui et relâché).
  const phase = useRef<'repos' | 'demarrage' | 'enregistrement' | 'arret'>('repos');
  const relache = useRef(false);
  const debutEnregistrement = useRef(0);
  const [enregistre, setEnregistre] = useState(false);
  const [micro, setMicro] = useState<'inconnu' | 'autorise' | 'refuse'>('inconnu');

  // Demande l'accès au micro à l'ouverture : la fenêtre d'iOS n'interrompt plus l'appui sur le bouton.
  useEffect(() => {
    if (!vocalAutorise) return;
    AudioModule.requestRecordingPermissionsAsync()
      .then((p) => setMicro(p.granted ? 'autorise' : 'refuse'))
      .catch(() => setMicro('refuse'));
  }, [vocalAutorise]);

  const commencerEnregistrement = async () => {
    if (phase.current !== 'repos') return;
    setMessage(null);
    relache.current = false;
    if (micro !== 'autorise') {
      const permission = await AudioModule.requestRecordingPermissionsAsync();
      setMicro(permission.granted ? 'autorise' : 'refuse');
      setMessage(permission.granted
        ? { type: 'info', texte: 'Micro autorisé. Maintenez le bouton et parlez.' }
        : { type: 'erreur', texte: 'Autorisez le micro dans Réglages > Wiri-Wiri Shop (ou Expo Go).' });
      return;
    }
    phase.current = 'demarrage';
    try {
      await setAudioModeAsync({ allowsRecording: true, playsInSilentMode: true });
      await enregistreur.prepareToRecordAsync();
      if (relache.current) { // doigt relâché pendant la préparation du micro
        phase.current = 'repos';
        await setAudioModeAsync({ allowsRecording: false });
        setMessage({ type: 'info', texte: 'Gardez le doigt sur le bouton pendant que vous parlez.' });
        return;
      }
      enregistreur.record();
      debutEnregistrement.current = Date.now();
      phase.current = 'enregistrement';
      setEnregistre(true);
      vibrer();
    } catch {
      phase.current = 'repos';
      setMessage({ type: 'erreur', texte: 'Impossible d\'utiliser le micro. Fermez les autres applications qui l\'utilisent.' });
    }
  };

  const terminerEnregistrement = async () => {
    relache.current = true;
    if (phase.current !== 'enregistrement') return; // la préparation gère elle-même le relâché
    phase.current = 'arret';
    const duree = Date.now() - debutEnregistrement.current;
    try {
      await enregistreur.stop();
    } finally {
      await setAudioModeAsync({ allowsRecording: false }).catch(() => undefined);
      setEnregistre(false);
      phase.current = 'repos';
    }
    vibrer();
    const uri = enregistreur.uri;
    if (duree < 800 || !uri) {
      setMessage({ type: 'info', texte: 'Trop court : maintenez le bouton et décrivez l\'article.' });
      return;
    }
    setAttente('analyse');
    try {
      const resultat = await api.analyserNoteVocale(uri);
      setTranscription(resultat.transcription);
      if (resultat.nomProduit) setNom(resultat.nomProduit);
      if (resultat.prix) setPrix(String(resultat.prix));
      if (resultat.taille) setTaille(resultat.taille);
      if (!resultat.transcription) setMessage({ type: 'info', texte: 'Je n\'ai rien entendu. Parlez plus près du téléphone.' });
      else if (!resultat.complet) setMessage({ type: 'info', texte: 'Je n\'ai pas tout compris : complétez les champs vides.' });
    } catch (e) {
      setMessage({ type: 'erreur', texte: e instanceof ErreurApi ? e.message : 'Connexion impossible. Réessayez.' });
    }
    setAttente(null);
  };

  const montant = Number(prix);
  const valide = !!photo && nom.trim().length > 0 && Number.isInteger(montant) && montant > 0;

  const publier = async () => {
    if (!photo || !valide) return;
    setAttente('publication');
    setMessage(null);
    try {
      await api.publierProduit({
        nomProduit: nom.trim(),
        prix: montant,
        taille: taille.trim() || undefined,
        uriImage: photo.uri,
        typeImage: photo.mimeType ?? 'image/jpeg',
      });
      onTermine();
    } catch (e) {
      setMessage({ type: 'erreur', texte: e instanceof ErreurApi ? e.message : 'Connexion impossible. Réessayez.' });
      setAttente(null);
    }
  };

  return (
    <Ecran pied={
      <Bouton titre="Publier l'article" icone="checkmark-circle" onPress={publier} desactive={!valide || attente !== null}
              chargement={attente === 'publication'} testID="bouton-publier" />
    }>
      <EnTete titre="Nouvel article" onRetour={onTermine} />

      {/* 1. Photo */}
      <Pressable onPress={prendrePhoto} accessibilityRole="button" accessibilityLabel="Prendre une photo"
                 style={({ pressed }) => [styles.photo, !photo && styles.photoVide, pressed && { opacity: 0.9 }]}>
        {photo ? (
          <>
            <Image source={{ uri: photo.uri }} style={StyleSheet.absoluteFill} />
            <View style={styles.reprendre}>
              <Ionicons name="camera-reverse-outline" size={16} color={couleurs.texte} />
              <Text style={styles.reprendreTexte}>Reprendre</Text>
            </View>
          </>
        ) : (
          <View style={{ alignItems: 'center', gap: espace.s }}>
            <Pictogramme icone="camera" taille={64} />
            <Text style={typo.h3}>Prendre une photo</Text>
            <Text style={typo.petit}>Fond clair, article bien centré</Text>
          </View>
        )}
      </Pressable>

      {/* 2. Voix */}
      {vocalAutorise ? (
        <Carte style={styles.voix}>
          <BoutonMicro enregistre={enregistre} occupe={attente !== null}
                       onDebut={commencerEnregistrement} onFin={terminerEnregistrement} />
          <View style={{ flex: 1, gap: 4 }}>
            {attente === 'analyse' ? (
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: espace.s }}>
                <ActivityIndicator color={couleurs.marque} />
                <Text style={typo.h3}>Analyse en cours…</Text>
              </View>
            ) : enregistre ? (
              <>
                <Text style={[typo.h3, { color: couleurs.enregistrement }]}>
                  Je vous écoute · {Math.round(etatEnregistreur.durationMillis / 1000)} s
                </Text>
                <Text style={typo.petit}>Relâchez quand vous avez fini</Text>
              </>
            ) : (
              <>
                <Text style={typo.h3}>Décrivez en wolof</Text>
                <Text style={typo.petit}>Maintenez le micro : nom, prix et taille de l&apos;article</Text>
              </>
            )}
          </View>
        </Carte>
      ) : (
        <Pressable onPress={onDemanderConsentement} accessibilityRole="button">
          <Carte style={styles.voix}>
            <Pictogramme icone="mic-off-outline" taille={48} fond={couleurs.fond} teinte={couleurs.texte2} />
            <View style={{ flex: 1, gap: 2 }}>
              <Text style={typo.h3}>Saisie vocale désactivée</Text>
              <Text style={[typo.petit, { color: couleurs.marque, fontWeight: '600' }]}>Activer →</Text>
            </View>
          </Carte>
        </Pressable>
      )}

      {transcription ? (
        <View style={styles.transcription}>
          <Ionicons name="chatbubble-ellipses-outline" size={18} color={couleurs.texte2} />
          <Text style={[typo.corps, { flex: 1, fontStyle: 'italic', fontSize: 15 }]}>« {transcription} »</Text>
        </View>
      ) : null}

      {message && <Message type={message.type} texte={message.texte} />}

      {/* 3. Fiche */}
      <View style={{ gap: espace.l }}>
        <Champ label="Nom de l'article" placeholder="Ex. Robe wax" value={nom} onChangeText={setNom} maxLength={150}
               testID="champ-nom-produit" />
        <View style={{ flexDirection: 'row', gap: espace.m }}>
          <View style={{ flex: 3 }}>
            <Champ label="Prix" placeholder="15 000" keyboardType="number-pad" suffixe="FCFA" maxLength={11}
                   value={prix.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} onChangeText={(t) => setPrix(t.replace(/\D/g, ''))} testID="champ-prix" />
          </View>
          <View style={{ flex: 2 }}>
            <Champ label="Taille" placeholder="M, 42…" value={taille} onChangeText={setTaille} maxLength={20} />
          </View>
        </View>
      </View>
    </Ecran>
  );
}

/** Gros bouton rond « appuyer pour parler », avec une onde qui pulse pendant l'enregistrement. */
function BoutonMicro({ enregistre, occupe, onDebut, onFin }: {
  enregistre: boolean;
  occupe: boolean;
  onDebut: () => void;
  onFin: () => void;
}) {
  const onde = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!enregistre) {
      onde.stopAnimation();
      onde.setValue(0);
      return;
    }
    const boucle = Animated.loop(Animated.timing(onde, {
      toValue: 1, duration: 1200, easing: Easing.out(Easing.ease), useNativeDriver: true,
    }));
    boucle.start();
    return () => boucle.stop();
  }, [enregistre, onde]);

  const couleur = enregistre ? couleurs.enregistrement : couleurs.marque;
  return (
    <Pressable onPressIn={onDebut} onPressOut={onFin} disabled={occupe} accessibilityRole="button"
               accessibilityLabel="Maintenir pour parler" style={{ width: 72, height: 72 }}>
      <Animated.View pointerEvents="none" style={[styles.onde, {
        backgroundColor: couleur,
        opacity: onde.interpolate({ inputRange: [0, 1], outputRange: [0.35, 0] }),
        transform: [{ scale: onde.interpolate({ inputRange: [0, 1], outputRange: [1, 1.7] }) }],
      }]} />
      <LinearGradient colors={enregistre ? [couleurs.enregistrement, '#B42318'] : [couleurs.marque, couleurs.marqueFonce]}
                      style={[styles.micro, ombre.forte, occupe && { opacity: 0.5 }]}>
        <Ionicons name={enregistre ? 'radio-button-on' : 'mic'} size={32} color={couleurs.blanc} />
      </LinearGradient>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  photo: {
    aspectRatio: 1,
    borderRadius: rayon.l,
    overflow: 'hidden',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: couleurs.surface,
  },
  photoVide: { borderWidth: 2, borderStyle: 'dashed', borderColor: couleurs.bordFort },
  reprendre: {
    position: 'absolute',
    bottom: espace.m,
    right: espace.m,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: espace.m,
    paddingVertical: espace.s,
    borderRadius: rayon.rond,
    backgroundColor: 'rgba(255,255,255,0.95)',
  },
  reprendreTexte: { fontSize: 14, fontWeight: '600', color: couleurs.texte },
  voix: { flexDirection: 'row', alignItems: 'center', gap: espace.l },
  onde: { position: 'absolute', width: 72, height: 72, borderRadius: 36 },
  micro: { width: 72, height: 72, borderRadius: 36, alignItems: 'center', justifyContent: 'center' },
  transcription: {
    flexDirection: 'row',
    gap: espace.s,
    padding: espace.m,
    borderRadius: rayon.m,
    backgroundColor: couleurs.surface,
    borderLeftWidth: 3,
    borderLeftColor: couleurs.accent,
  },
});
