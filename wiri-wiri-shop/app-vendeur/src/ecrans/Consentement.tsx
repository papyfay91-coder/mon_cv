import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { api, Boutique } from '../api';
import { espace, typo } from '../theme';
import { Bouton, Carte, Ecran, Message, NomIcone, Pictogramme } from '../ui';

const POINTS: { icone: NomIcone; titre: string; texte: string }[] = [
  { icone: 'mic-outline', titre: 'Un seul usage', texte: 'Le micro sert uniquement à remplir la fiche de votre article : nom, prix, taille.' },
  { icone: 'cloud-outline', titre: 'Transcription par IA', texte: 'Votre note vocale est transcrite par OpenAI, sur des serveurs situés hors du Sénégal.' },
  { icone: 'trash-outline', titre: 'Jamais conservée', texte: 'L\'enregistrement est supprimé dès que la fiche est remplie.' },
  { icone: 'options-outline', titre: 'Vous gardez la main', texte: 'Vous vérifiez chaque fiche, et pouvez retirer votre accord à tout moment dans les Paramètres.' },
];

/**
 * Consentement explicite et éclairé (CDP) AVANT toute lecture du micro ou envoi d'audio.
 * Le vendeur peut refuser : il saisira alors ses fiches à la main.
 */
export default function Consentement({ onAccepte, onRefuse }: {
  onAccepte: (b: Boutique) => void;
  onRefuse: () => void;
}) {
  const [attente, setAttente] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

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

      <Carte style={{ gap: espace.l }}>
        {POINTS.map((p) => (
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

const styles = StyleSheet.create({
  ligne: { flexDirection: 'row', gap: espace.m, alignItems: 'flex-start' },
});

