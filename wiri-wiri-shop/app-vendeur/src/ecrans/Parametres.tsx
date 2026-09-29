import { useState } from 'react';
import { Alert, Pressable, ScrollView, Text, TextInput } from 'react-native';
import { api, Boutique, ErreurApi } from '../api';
import { couleurs, styles } from '../theme';

export default function Parametres({ boutique, onMiseAJour, onRetour, onDeconnexion }: {
  boutique: Boutique;
  onMiseAJour: (b: Boutique) => void;
  onRetour: () => void;
  onDeconnexion: () => void;
}) {
  const [nom, setNom] = useState(boutique.nomVendeur);
  const [lienWave, setLienWave] = useState(boutique.lienWave ?? '');
  const [message, setMessage] = useState<string | null>(null);

  const enregistrer = async () => {
    try {
      onMiseAJour(await api.mettreAJour({ nomVendeur: nom.trim(), lienWave: lienWave.trim() }));
      setMessage('Enregistré ✅');
    } catch (e) {
      setMessage(e instanceof ErreurApi ? e.message : 'Pas de connexion internet.');
    }
  };

  const retirerConsentement = async () => {
    onMiseAJour(await api.retirerConsentement());
    setMessage('Saisie vocale désactivée.');
  };

  /** Droit à l'oubli : suppression définitive, après double confirmation. */
  const supprimerCompte = () =>
    Alert.alert(
      'Supprimer mon compte ?',
      'Votre boutique, tous vos articles, photos et commandes seront effacés définitivement.',
      [
        { text: 'Annuler', style: 'cancel' },
        {
          text: 'Tout supprimer',
          style: 'destructive',
          onPress: async () => {
            await api.supprimerCompte();
            onDeconnexion();
          },
        },
      ],
    );

  return (
    <ScrollView contentContainerStyle={styles.ecran}>
      <Pressable onPress={onRetour} hitSlop={16}><Text style={styles.doux}>← Retour</Text></Pressable>
      <Text style={styles.titre}>⚙️ Paramètres</Text>

      <Text style={styles.texte}>🏪 Nom de la boutique</Text>
      <TextInput style={styles.champ} value={nom} onChangeText={setNom} maxLength={100} />

      <Text style={styles.texte}>🌊 Mon lien de paiement Wave</Text>
      <TextInput style={styles.champ} value={lienWave} onChangeText={setLienWave} autoCapitalize="none"
                 keyboardType="url" placeholder="https://pay.wave.com/m/…" />
      <Text style={styles.doux}>Vos clients vous paient directement. Wiri-Wiri Shop ne touche jamais l&apos;argent.</Text>

      <Pressable style={styles.bouton} onPress={enregistrer}>
        <Text style={styles.boutonTexte}>Enregistrer</Text>
      </Pressable>
      {message && <Text style={styles.doux}>{message}</Text>}

      {boutique.consentementDonne && (
        <Pressable style={styles.boutonSecondaire} onPress={retirerConsentement}>
          <Text style={styles.boutonSecondaireTexte}>🎙️ Désactiver la saisie vocale</Text>
        </Pressable>
      )}
      <Pressable style={styles.boutonSecondaire} onPress={onDeconnexion}>
        <Text style={styles.boutonSecondaireTexte}>Se déconnecter</Text>
      </Pressable>
      <Pressable style={[styles.boutonSecondaire, { borderColor: couleurs.danger }]} onPress={supprimerCompte}>
        <Text style={[styles.boutonSecondaireTexte, { color: couleurs.danger }]}>🗑️ Supprimer mon compte et mes données</Text>
      </Pressable>
    </ScrollView>
  );
}
