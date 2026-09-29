import {
  AudioModule,
  RecordingPresets,
  setAudioModeAsync,
  useAudioRecorder,
  useAudioRecorderState,
} from 'expo-audio';
import * as ImagePicker from 'expo-image-picker';
import { useState } from 'react';
import { ActivityIndicator, Image, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { api, ErreurApi } from '../api';
import { couleurs, styles } from '../theme';

/**
 * Création d'une fiche en ~5 secondes :
 * 1. 📸 photo de l'article ;
 * 2. 🎙️ le vendeur décrit l'article en wolof (appui maintenu) → l'IA pré-remplit nom / prix / taille ;
 * 3. ✅ le vendeur vérifie et publie.
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
  const [erreur, setErreur] = useState<string | null>(null);

  const prendrePhoto = async () => {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) return setErreur('Autorisez l\'appareil photo dans les réglages.');
    const resultat = await ImagePicker.launchCameraAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.6, // image légère pour les réseaux 3G
      exif: false, // pas de métadonnées (localisation) envoyées
    });
    if (!resultat.canceled) setPhoto(resultat.assets[0]);
  };

  const commencerEnregistrement = async () => {
    setErreur(null);
    const permission = await AudioModule.requestRecordingPermissionsAsync();
    if (!permission.granted) return setErreur('Autorisez le micro dans les réglages.');
    await setAudioModeAsync({ allowsRecording: true, playsInSilentMode: true });
    await enregistreur.prepareToRecordAsync();
    enregistreur.record();
  };

  const terminerEnregistrement = async () => {
    if (!etatEnregistreur.isRecording) return;
    await enregistreur.stop();
    await setAudioModeAsync({ allowsRecording: false });
    const uri = enregistreur.uri;
    if (!uri) return;
    setAttente('analyse');
    try {
      const resultat = await api.analyserNoteVocale(uri);
      setTranscription(resultat.transcription);
      if (resultat.nomProduit) setNom(resultat.nomProduit);
      if (resultat.prix) setPrix(String(resultat.prix));
      if (resultat.taille) setTaille(resultat.taille);
      if (!resultat.complet) setErreur('Je n\'ai pas tout compris : complétez les champs vides.');
    } catch (e) {
      setErreur(e instanceof ErreurApi ? e.message : 'Pas de connexion internet.');
    }
    setAttente(null);
  };

  const publier = async () => {
    const montant = Number(prix);
    if (!photo || nom.trim().length === 0 || !Number.isInteger(montant) || montant <= 0) {
      return setErreur('Il faut une photo, un nom et un prix.');
    }
    setAttente('publication');
    setErreur(null);
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
      setErreur(e instanceof ErreurApi ? e.message : 'Pas de connexion internet.');
      setAttente(null);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.ecran} keyboardShouldPersistTaps="handled">
      <Pressable onPress={onTermine} hitSlop={16}><Text style={styles.doux}>← Retour</Text></Pressable>

      <Pressable onPress={prendrePhoto}
                 style={{ aspectRatio: 1, borderRadius: 16, backgroundColor: couleurs.bord, overflow: 'hidden',
                          alignItems: 'center', justifyContent: 'center' }}>
        {photo ? <Image source={{ uri: photo.uri }} style={{ width: '100%', height: '100%' }} />
               : <Text style={{ fontSize: 64 }}>📸</Text>}
      </Pressable>

      {vocalAutorise ? (
        <Pressable
          onPressIn={commencerEnregistrement}
          onPressOut={terminerEnregistrement}
          disabled={attente !== null}
          style={[styles.bouton, { paddingVertical: 26 },
                  etatEnregistreur.isRecording && { backgroundColor: couleurs.enregistrement }]}
          accessibilityLabel="Maintenir pour parler">
          <Text style={[styles.boutonTexte, { fontSize: 22 }]}>
            {etatEnregistreur.isRecording
              ? `🔴 Je vous écoute… ${Math.round(etatEnregistreur.durationMillis / 1000)} s`
              : '🎙️ Maintenez et décrivez'}
          </Text>
        </Pressable>
      ) : (
        <Pressable style={styles.boutonSecondaire} onPress={onDemanderConsentement}>
          <Text style={styles.boutonSecondaireTexte}>🎙️ Activer la saisie vocale</Text>
        </Pressable>
      )}

      {attente === 'analyse' && (
        <View style={{ flexDirection: 'row', gap: 10, alignItems: 'center' }}>
          <ActivityIndicator color={couleurs.accent} />
          <Text style={styles.doux}>Analyse de votre voix…</Text>
        </View>
      )}
      {transcription ? <Text style={styles.doux}>« {transcription} »</Text> : null}

      <TextInput style={styles.champ} placeholder="🏷️ Nom de l'article" value={nom} onChangeText={setNom} maxLength={150} />
      <TextInput style={styles.champ} placeholder="💰 Prix (FCFA)" keyboardType="number-pad" value={prix}
                 onChangeText={(t) => setPrix(t.replace(/\D/g, ''))} maxLength={9} />
      <TextInput style={styles.champ} placeholder="📏 Taille (facultatif)" value={taille} onChangeText={setTaille}
                 maxLength={20} />

      {erreur && <Text style={styles.erreur}>{erreur}</Text>}

      <Pressable style={styles.bouton} onPress={publier} disabled={attente !== null}>
        {attente === 'publication' ? <ActivityIndicator color="#fff" />
                                   : <Text style={styles.boutonTexte}>✅ Publier</Text>}
      </Pressable>
    </ScrollView>
  );
}
