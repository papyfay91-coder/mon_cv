import { useState } from 'react';
import { ActivityIndicator, Pressable, Text, TextInput, View } from 'react-native';
import { api, ErreurApi, jeton } from '../api';
import { styles } from '../theme';

/** Connexion sans mot de passe : numéro de téléphone puis code reçu par SMS. */
export default function Connexion({ onConnecte }: { onConnecte: () => void }) {
  const [etape, setEtape] = useState<'telephone' | 'code' | 'nom'>('telephone');
  const [telephone, setTelephone] = useState('');
  const [code, setCode] = useState('');
  const [nom, setNom] = useState('');
  const [attente, setAttente] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const executer = async (action: () => Promise<void>) => {
    setErreur(null);
    setAttente(true);
    try {
      await action();
    } catch (e) {
      if (e instanceof ErreurApi && e.code === 'NOM_REQUIS') {
        setEtape('nom');
      } else if (e instanceof ErreurApi && e.status === 429) {
        setErreur('Trop d\'essais. Patientez une minute.');
      } else {
        setErreur(e instanceof ErreurApi ? e.message : 'Pas de connexion internet.');
      }
    } finally {
      setAttente(false);
    }
  };

  const envoyerCode = () =>
    executer(async () => {
      await api.demanderCode(telephone);
      setEtape('code');
    });

  const verifier = () =>
    executer(async () => {
      const reponse = await api.verifierCode(telephone, code, etape === 'nom' ? nom : undefined);
      await jeton.ecrire(reponse.jeton);
      onConnecte();
    });

  return (
    <View style={styles.ecran}>
      <Text style={{ fontSize: 56, textAlign: 'center' }}>🛍️</Text>
      <Text style={[styles.titre, { textAlign: 'center' }]}>Wiri-Wiri Shop</Text>

      {etape === 'telephone' && (
        <>
          <Text style={styles.texte}>📱 Votre numéro</Text>
          <TextInput style={styles.champ} keyboardType="phone-pad" autoComplete="tel" placeholder="77 123 45 67"
                     value={telephone} onChangeText={setTelephone} maxLength={20} />
          <Pressable style={styles.bouton} onPress={envoyerCode} disabled={attente || telephone.length < 9}>
            <Text style={styles.boutonTexte}>Recevoir le code ✉️</Text>
          </Pressable>
        </>
      )}

      {etape !== 'telephone' && (
        <>
          <Text style={styles.texte}>🔢 Code reçu par SMS</Text>
          <TextInput style={[styles.champ, { letterSpacing: 8, textAlign: 'center' }]} keyboardType="number-pad"
                     autoComplete="sms-otp" textContentType="oneTimeCode" maxLength={6}
                     value={code} onChangeText={(t) => setCode(t.replace(/\D/g, ''))} />
        </>
      )}

      {etape === 'nom' && (
        <>
          <Text style={styles.texte}>🏪 Nom de votre boutique</Text>
          <TextInput style={styles.champ} placeholder="Awa Fashion" value={nom} onChangeText={setNom} maxLength={100} />
        </>
      )}

      {etape !== 'telephone' && (
        <Pressable style={styles.bouton} onPress={verifier}
                   disabled={attente || code.length !== 6 || (etape === 'nom' && nom.trim().length < 2)}>
          <Text style={styles.boutonTexte}>Entrer ✅</Text>
        </Pressable>
      )}

      {attente && <ActivityIndicator />}
      {erreur && <Text style={styles.erreur}>{erreur}</Text>}
    </View>
  );
}
