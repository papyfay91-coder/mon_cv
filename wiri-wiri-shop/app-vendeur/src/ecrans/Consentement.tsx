import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { api, Boutique } from '../api';
import { styles } from '../theme';

/**
 * Consentement explicite et éclairé (CDP) AVANT toute lecture du micro ou envoi d'audio.
 * Le vendeur peut refuser : il saisira alors ses fiches à la main.
 */
export default function Consentement({ onAccepte, onRefuse }: {
  onAccepte: (b: Boutique) => void;
  onRefuse: () => void;
}) {
  const [erreur, setErreur] = useState<string | null>(null);

  const accepter = async () => {
    try {
      onAccepte(await api.consentir());
    } catch {
      setErreur('Pas de connexion internet. Réessayez.');
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.ecran}>
      <Text style={{ fontSize: 48, textAlign: 'center' }}>🎙️🔒</Text>
      <Text style={styles.titre}>Votre voix, vos données</Text>
      <View style={{ gap: 12 }}>
        <Text style={styles.texte}>• Le micro sert <Text style={{ fontWeight: '700' }}>uniquement</Text> à remplir la fiche de votre article (nom, prix, taille).</Text>
        <Text style={styles.texte}>• Votre note vocale est transcrite par un service d&apos;intelligence artificielle (OpenAI, serveurs situés hors du Sénégal), puis <Text style={{ fontWeight: '700' }}>supprimée</Text> : elle n&apos;est jamais conservée.</Text>
        <Text style={styles.texte}>• Vous vérifiez toujours la fiche avant de la publier.</Text>
        <Text style={styles.texte}>• Vous pouvez retirer votre accord, ou supprimer votre compte et toutes vos données, à tout moment dans ⚙️ Paramètres.</Text>
        <Text style={styles.doux}>Traitement conforme à la loi sénégalaise n° 2008-12 sur les données personnelles (CDP).</Text>
      </View>
      <Pressable style={styles.bouton} onPress={accepter}>
        <Text style={styles.boutonTexte}>J&apos;ai compris et j&apos;accepte ✅</Text>
      </Pressable>
      <Pressable style={styles.boutonSecondaire} onPress={onRefuse}>
        <Text style={styles.boutonSecondaireTexte}>Non merci, je remplis à la main</Text>
      </Pressable>
      {erreur && <Text style={styles.erreur}>{erreur}</Text>}
    </ScrollView>
  );
}
