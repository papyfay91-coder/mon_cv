import { Ionicons } from '@expo/vector-icons';
import { ReactNode, useState } from 'react';
import { Alert, Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { api, Boutique, ErreurApi } from '../api';
import { couleurs, espace, rayon, typo } from '../theme';
import { Bouton, Carte, Champ, Ecran, EnTete, Message, NomIcone, Pictogramme, Section } from '../ui';

export default function Parametres({ boutique, onMiseAJour, onRetour, onDeconnexion }: {
  boutique: Boutique;
  onMiseAJour: (b: Boutique) => void;
  onRetour: () => void;
  onDeconnexion: () => void;
}) {
  const [nom, setNom] = useState(boutique.nomVendeur);
  const [lienWave, setLienWave] = useState(boutique.lienWave ?? '');
  const [attente, setAttente] = useState(false);
  const [message, setMessage] = useState<{ type: 'erreur' | 'succes'; texte: string } | null>(null);

  const modifie = nom.trim() !== boutique.nomVendeur || lienWave.trim() !== (boutique.lienWave ?? '');

  const enregistrer = async () => {
    setAttente(true);
    setMessage(null);
    try {
      onMiseAJour(await api.mettreAJour({ nomVendeur: nom.trim(), lienWave: lienWave.trim() }));
      setMessage({ type: 'succes', texte: 'Modifications enregistrées.' });
    } catch (e) {
      setMessage({ type: 'erreur', texte: e instanceof ErreurApi ? e.message : 'Connexion impossible.' });
    }
    setAttente(false);
  };

  const basculerVoix = async (active: boolean) => {
    onMiseAJour(await (active ? api.consentir() : api.retirerConsentement()));
  };

  /** Droit à l'oubli : suppression définitive, après confirmation. */
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

  const initiales = boutique.nomVendeur.split(/\s+/).map((m) => m[0]).join('').slice(0, 2).toUpperCase();

  return (
    <Ecran>
      <EnTete titre="Paramètres" onRetour={onRetour} />

      <Carte style={styles.profil}>
        <View style={styles.avatar}><Text style={styles.initiales}>{initiales}</Text></View>
        <View style={{ flex: 1 }}>
          <Text style={typo.h3} numberOfLines={1}>{boutique.nomVendeur}</Text>
          <Text style={typo.petit}>{boutique.telephone.replace(/^\+221(\d{2})(\d{3})(\d{2})(\d{2})$/, '+221 $1 $2 $3 $4')}</Text>
        </View>
      </Carte>

      <Section titre="Boutique">
        <Carte style={{ gap: espace.l }}>
          <Champ label="Nom de la boutique" value={nom} onChangeText={setNom} maxLength={100} />
          <Champ label="Lien de paiement Wave" value={lienWave} onChangeText={setLienWave} autoCapitalize="none"
                 keyboardType="url" placeholder="https://pay.wave.com/m/…"
                 aide="Vos clients vous paient directement. Wiri-Wiri Shop ne touche jamais l'argent." />
          {message && <Message type={message.type} texte={message.texte} />}
          <Bouton titre="Enregistrer" variante="secondaire" onPress={enregistrer} chargement={attente} desactive={!modifie} />
        </Carte>
      </Section>

      <Section titre="Confidentialité">
        <Carte style={{ paddingVertical: espace.s }}>
          <Ligne icone="mic-outline" titre="Saisie vocale" sousTitre="Décrire les articles à voix haute">
            <Switch value={boutique.consentementDonne} onValueChange={basculerVoix}
                    trackColor={{ true: couleurs.marque, false: couleurs.bord }} thumbColor={couleurs.blanc} />
          </Ligne>
        </Carte>
      </Section>

      <Section titre="Compte">
        <Carte style={{ paddingVertical: espace.s }}>
          <Ligne icone="log-out-outline" titre="Se déconnecter" onPress={onDeconnexion} />
          <View style={styles.trait} />
          <Ligne icone="trash-outline" titre="Supprimer mon compte" sousTitre="Efface toutes vos données" danger
                 onPress={supprimerCompte} />
        </Carte>
      </Section>

      <Text style={[typo.petit, { textAlign: 'center' }]}>Wiri-Wiri Shop · version 0.1.0</Text>
    </Ecran>
  );
}

function Ligne({ icone, titre, sousTitre, danger, onPress, children }: {
  icone: NomIcone;
  titre: string;
  sousTitre?: string;
  danger?: boolean;
  onPress?: () => void;
  children?: ReactNode;
}) {
  const teinte = danger ? couleurs.danger : couleurs.marque;
  return (
    <Pressable onPress={onPress} disabled={!onPress} accessibilityRole={onPress ? 'button' : undefined}
               style={({ pressed }) => [styles.ligne, pressed && { opacity: 0.6 }]}>
      <Pictogramme icone={icone} taille={38} fond={danger ? couleurs.dangerClair : couleurs.marqueClair} teinte={teinte} />
      <View style={{ flex: 1 }}>
        <Text style={[typo.h3, { fontWeight: '600' }, danger && { color: couleurs.danger }]}>{titre}</Text>
        {sousTitre && <Text style={typo.petit}>{sousTitre}</Text>}
      </View>
      {children ?? (onPress && <Ionicons name="chevron-forward" size={18} color={couleurs.texte3} />)}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  profil: { flexDirection: 'row', alignItems: 'center', gap: espace.l },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: rayon.l,
    backgroundColor: couleurs.marque,
    alignItems: 'center',
    justifyContent: 'center',
  },
  initiales: { fontSize: 20, fontWeight: '800', color: couleurs.blanc },
  ligne: { flexDirection: 'row', alignItems: 'center', gap: espace.m, paddingVertical: espace.s },
  trait: { height: StyleSheet.hairlineWidth, backgroundColor: couleurs.bord, marginLeft: 50 },
});
