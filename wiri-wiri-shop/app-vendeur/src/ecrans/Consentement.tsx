import { Ionicons } from '@expo/vector-icons';
import { AudioSource, setAudioModeAsync, useAudioPlayer, useAudioPlayerStatus } from 'expo-audio';
import { LinearGradient } from 'expo-linear-gradient';
import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { api, Boutique, TraitementVocal } from '../api';
import { CONDITIONS_WOLOF } from '../audio/conditions';
import { couleurs, espace, ombre, rayon, typo } from '../theme';
import { Bouton, Carte, Ecran, Message, NomIcone, Pictogramme, vibrer } from '../ui';

type Point = { icone: NomIcone; titre: string; texte: string };

/**
 * Le texte décrit le traitement réellement configuré sur le serveur (CDP : information exacte).
 * Sans information du serveur, on affiche le cas le plus large (service tiers hors du Sénégal).
 */
function lieuDeTraitement(traitement?: TraitementVocal): Point {
  if (traitement?.hebergement === 'LOCAL') {
    return {
      icone: 'server-outline',
      titre: 'Traitée chez nous',
      texte: 'Votre note vocale est transcrite par notre propre IA, sur nos serveurs. Elle n\'est envoyée à aucun service extérieur.',
    };
  }
  const fournisseur = traitement?.fournisseur ?? 'un service d\'intelligence artificielle';
  return {
    icone: 'cloud-outline',
    titre: 'Transcription par IA',
    texte: `Votre note vocale est transcrite par ${fournisseur}, sur des serveurs situés hors du Sénégal.`,
  };
}

const points = (traitement?: TraitementVocal): Point[] => [
  { icone: 'mic-outline', titre: 'Un seul usage', texte: 'Le micro sert uniquement à remplir la fiche de votre article : nom, prix, taille.' },
  lieuDeTraitement(traitement),
  { icone: 'trash-outline', titre: 'Jamais conservée', texte: 'L\'enregistrement est supprimé dès que la fiche est remplie.' },
  { icone: 'options-outline', titre: 'Vous gardez la main', texte: 'Vous vérifiez chaque fiche, et pouvez retirer votre accord à tout moment dans les Paramètres.' },
];

/**
 * Consentement explicite et éclairé (CDP) AVANT toute lecture du micro ou envoi d'audio.
 * Le vendeur peut refuser : il saisira alors ses fiches à la main.
 */
export default function Consentement({ traitement, onAccepte, onRefuse }: {
  traitement?: TraitementVocal;
  onAccepte: (b: Boutique) => void;
  onRefuse: () => void;
}) {
  const [attente, setAttente] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const audioWolof = CONDITIONS_WOLOF[traitement?.hebergement === 'LOCAL' ? 'LOCAL' : 'EXTERNE'];

  const accepter = async () => {
    setAttente(true);
    setErreur(null);
    try {
      onAccepte(await api.consentir());
    } catch {
      setErreur('Connexion impossible. Réessayez.');
      setAttente(false);
    }
  };

  return (
    <Ecran pied={
      <View style={{ gap: espace.s }}>
        <Bouton titre="J'accepte et je continue" onPress={accepter} chargement={attente} testID="bouton-accepter" />
        <Bouton titre="Continuer sans la voix" variante="fantome" onPress={onRefuse} />
      </View>
    }>
      <View style={{ alignItems: 'center', gap: espace.m, paddingTop: espace.l }}>
        <Pictogramme icone="shield-checkmark" taille={72} />
        <Text style={[typo.titre, { textAlign: 'center' }]}>Votre voix, vos données</Text>
        <Text style={[typo.corps, { textAlign: 'center' }]}>
          Décrivez vos articles à voix haute, en wolof. Voici comment votre voix est utilisée.
        </Text>
      </View>

      {audioWolof ? (
        <LecteurWolof source={audioWolof} />
      ) : __DEV__ ? (
        <Message type="info" texte="Version audio en wolof pas encore enregistrée : voir assets/audio/LISEZMOI.md." />
      ) : null}

      <Carte style={{ gap: espace.l }}>
        {points(traitement).map((p) => (
          <View key={p.titre} style={styles.ligne}>
            <Pictogramme icone={p.icone} taille={40} />
            <View style={{ flex: 1, gap: 2 }}>
              <Text style={typo.h3}>{p.titre}</Text>
              <Text style={[typo.corps, { fontSize: 15, lineHeight: 21 }]}>{p.texte}</Text>
            </View>
          </View>
        ))}
      </Carte>

      <Text style={[typo.petit, { textAlign: 'center' }]}>
        Traitement conforme à la loi sénégalaise n° 2008-12 sur les données personnelles (CDP).
      </Text>
      {erreur && <Message type="erreur" texte={erreur} />}
    </Ecran>
  );
}

/** Lecture des conditions en wolof : pour les vendeurs qui lisent peu le français. */
function LecteurWolof({ source }: { source: AudioSource }) {
  const lecteur = useAudioPlayer(source);
  const etat = useAudioPlayerStatus(lecteur);
  const progression = etat.duration > 0 ? Math.min(1, etat.currentTime / etat.duration) : 0;
  const termine = etat.didJustFinish || (etat.duration > 0 && etat.currentTime >= etat.duration - 0.2);

  const basculer = async () => {
    vibrer();
    if (etat.playing) {
      lecteur.pause();
      return;
    }
    await setAudioModeAsync({ playsInSilentMode: true }); // audible même en mode silencieux (iPhone)
    if (termine) await lecteur.seekTo(0);
    lecteur.play();
  };

  const minutes = (s: number) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, '0')}`;

  return (
    <Pressable onPress={basculer} accessibilityRole="button" testID="bouton-ecouter-wolof"
               accessibilityLabel={etat.playing ? 'Mettre en pause' : 'Écouter les conditions en wolof'}>
      <LinearGradient colors={[couleurs.marque, couleurs.marqueFonce]} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }}
                      style={[styles.lecteur, ombre.forte]}>
        <View style={styles.lecture}>
          <Ionicons name={etat.playing ? 'pause' : termine ? 'refresh' : 'play'} size={30} color={couleurs.marque} />
        </View>
        <View style={{ flex: 1, gap: 6 }}>
          <Text style={styles.lecteurTitre}>{termine && !etat.playing ? 'Réécouter en wolof' : 'Écouter en wolof'}</Text>
          <Text style={styles.lecteurSous}>Déglul ci wolof</Text>
          <View style={styles.barre}>
            <View style={[styles.barreRemplie, { width: `${progression * 100}%` }]} />
          </View>
          {etat.duration > 0 && (
            <Text style={styles.lecteurTemps}>{minutes(etat.currentTime)} / {minutes(etat.duration)}</Text>
          )}
        </View>
      </LinearGradient>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  lecteur: { flexDirection: 'row', alignItems: 'center', gap: espace.l, padding: espace.l, borderRadius: rayon.l },
  lecture: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: couleurs.blanc,
    alignItems: 'center',
    justifyContent: 'center',
  },
  lecteurTitre: { fontSize: 18, fontWeight: '800', color: couleurs.blanc },
  lecteurSous: { fontSize: 14, color: 'rgba(255,255,255,0.8)', marginTop: -4 },
  barre: { height: 6, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.25)', overflow: 'hidden' },
  barreRemplie: { height: 6, borderRadius: 3, backgroundColor: couleurs.accent },
  lecteurTemps: { fontSize: 12, color: 'rgba(255,255,255,0.8)', fontVariant: ['tabular-nums'] },
  ligne: { flexDirection: 'row', gap: espace.m, alignItems: 'flex-start' },
});

