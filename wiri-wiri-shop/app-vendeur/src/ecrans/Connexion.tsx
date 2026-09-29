import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useEffect, useRef, useState } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { api, ErreurApi, jeton } from '../api';
import { couleurs, espace, ombre, rayon, typo } from '../theme';
import { Bouton, Champ, Message } from '../ui';

type Etape = 'telephone' | 'code' | 'nom';

const TEXTES: Record<Etape, { titre: string; sousTitre: (tel: string) => string }> = {
  telephone: { titre: 'Connexion', sousTitre: () => 'Entrez votre numéro : nous vous envoyons un code par SMS. Aucun mot de passe.' },
  code: { titre: 'Vérification', sousTitre: (tel) => `Saisissez le code à 6 chiffres envoyé au +221 ${tel}.` },
  nom: { titre: 'Votre boutique', sousTitre: () => 'Dernière étape : comment s\'appelle votre boutique ?' },
};

/** Connexion sans mot de passe : numéro de téléphone puis code reçu par SMS. */
export default function Connexion({ onConnecte }: { onConnecte: () => void }) {
  const marges = useSafeAreaInsets();
  const [etape, setEtape] = useState<Etape>('telephone');
  const [telephone, setTelephone] = useState('');
  const [code, setCode] = useState('');
  const [nom, setNom] = useState('');
  const [attente, setAttente] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [delaiRenvoi, setDelaiRenvoi] = useState(0);

  useEffect(() => {
    if (delaiRenvoi <= 0) return;
    const t = setTimeout(() => setDelaiRenvoi((d) => d - 1), 1000);
    return () => clearTimeout(t);
  }, [delaiRenvoi]);

  const chiffres = telephone.replace(/\D/g, '');

  const executer = async (action: () => Promise<void>) => {
    setErreur(null);
    setAttente(true);
    try {
      await action();
    } catch (e) {
      if (e instanceof ErreurApi && e.code === 'NOM_REQUIS') setEtape('nom');
      else if (e instanceof ErreurApi && e.status === 429) setErreur('Trop d\'essais. Patientez une minute.');
      else if (e instanceof ErreurApi && e.code === 'CODE_INVALIDE') setErreur('Code incorrect ou expiré.');
      else setErreur(e instanceof ErreurApi ? e.message : 'Connexion impossible. Vérifiez votre réseau.');
    } finally {
      setAttente(false);
    }
  };

  const envoyerCode = () => executer(async () => {
    await api.demanderCode(`+221${chiffres}`);
    setCode('');
    setEtape('code');
    setDelaiRenvoi(60);
  });

  const verifier = () => executer(async () => {
    const reponse = await api.verifierCode(`+221${chiffres}`, code, etape === 'nom' ? nom.trim() : undefined);
    await jeton.ecrire(reponse.jeton);
    onConnecte();
  });

  const { titre, sousTitre } = TEXTES[etape];

  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: couleurs.marqueFonce }}
                          behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={{ flexGrow: 1 }} keyboardShouldPersistTaps="handled" bounces={false}>
        <LinearGradient colors={[couleurs.marque, couleurs.marqueFonce]} style={[styles.hero, { paddingTop: marges.top + 40 }]}>
          <View style={styles.logo}>
            <Ionicons name="bag-handle" size={34} color={couleurs.marque} />
          </View>
          <Text style={styles.marque}>Wiri-Wiri Shop</Text>
          <Text style={styles.slogan}>Votre boutique en ligne, en 5 secondes.</Text>
        </LinearGradient>

        <View style={[styles.feuille, { paddingBottom: marges.bottom + espace.xl }]}>
          <View style={{ gap: espace.s }}>
            <Text style={typo.titre}>{titre}</Text>
            <Text style={typo.corps}>{sousTitre(chiffres.replace(/(\d{2})(\d{3})(\d{2})(\d{2})/, '$1 $2 $3 $4'))}</Text>
          </View>

          {etape === 'telephone' && (
            <Champ label="Numéro de téléphone" prefixe="🇸🇳  +221" keyboardType="phone-pad" autoComplete="tel"
                   placeholder="77 123 45 67" maxLength={12} value={telephone} onChangeText={setTelephone}
                   testID="champ-telephone" />
          )}

          {etape !== 'telephone' && (
            <CodeOtp valeur={code} onChange={setCode} erreur={!!erreur && etape === 'code'} />
          )}

          {etape === 'nom' && (
            <Champ label="Nom de la boutique" placeholder="Ex. Awa Fashion" value={nom} onChangeText={setNom}
                   maxLength={100} autoFocus testID="champ-nom" />
          )}

          {erreur && <Message type="erreur" texte={erreur} />}

          {etape === 'telephone' ? (
            <Bouton titre="Recevoir le code" icone="arrow-forward" onPress={envoyerCode} chargement={attente}
                    desactive={chiffres.length !== 9} testID="bouton-code" />
          ) : (
            <Bouton titre={etape === 'nom' ? 'Créer ma boutique' : 'Valider'} onPress={verifier} chargement={attente}
                    desactive={code.length !== 6 || (etape === 'nom' && nom.trim().length < 2)} testID="bouton-valider" />
          )}

          {etape === 'code' && (
            <View style={styles.liens}>
              <Pressable onPress={() => { setEtape('telephone'); setErreur(null); }} hitSlop={8}>
                <Text style={styles.lien}>Modifier le numéro</Text>
              </Pressable>
              <Pressable onPress={envoyerCode} disabled={delaiRenvoi > 0} hitSlop={8}>
                <Text style={[styles.lien, delaiRenvoi > 0 && { color: couleurs.texte3 }]}>
                  {delaiRenvoi > 0 ? `Renvoyer (${delaiRenvoi} s)` : 'Renvoyer le code'}
                </Text>
              </Pressable>
            </View>
          )}

          <View style={{ flex: 1 }} />
          <View style={styles.securite}>
            <Ionicons name="lock-closed" size={14} color={couleurs.texte3} />
            <Text style={typo.petit}>Connexion chiffrée · Données protégées (CDP)</Text>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

/** Six cases pour le code : un seul champ invisible, affiché case par case. */
function CodeOtp({ valeur, onChange, erreur }: { valeur: string; onChange: (v: string) => void; erreur: boolean }) {
  const champ = useRef<TextInput>(null);
  const [focus, setFocus] = useState(true);
  return (
    <Pressable onPress={() => champ.current?.focus()} style={{ gap: espace.s }}>
      <Text style={typo.label}>Code de vérification</Text>
      <View style={styles.cases}>
        {Array.from({ length: 6 }, (_, i) => {
          const active = focus && i === Math.min(valeur.length, 5);
          return (
            <View key={i} style={[styles.case,
              valeur[i] && { borderColor: couleurs.bordFort },
              active && { borderColor: couleurs.marque, boxShadow: `0px 0px 0px 4px ${couleurs.marqueClair}` },
              erreur && { borderColor: couleurs.danger }]}>
              <Text style={styles.chiffre}>{valeur[i] ?? ''}</Text>
            </View>
          );
        })}
      </View>
      <TextInput
        ref={champ}
        testID="champ-code"
        value={valeur}
        onChangeText={(t) => onChange(t.replace(/\D/g, '').slice(0, 6))}
        onFocus={() => setFocus(true)}
        onBlur={() => setFocus(false)}
        keyboardType="number-pad"
        autoComplete="sms-otp"
        textContentType="oneTimeCode"
        maxLength={6}
        autoFocus
        caretHidden
        style={styles.champInvisible}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  hero: { alignItems: 'center', paddingBottom: 56, paddingHorizontal: espace.xl, gap: espace.s },
  logo: {
    width: 72,
    height: 72,
    borderRadius: 22,
    backgroundColor: couleurs.blanc,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: espace.s,
    ...ombre.moyenne,
  },
  marque: { fontSize: 26, fontWeight: '800', color: couleurs.blanc, letterSpacing: -0.5 },
  slogan: { fontSize: 15, color: 'rgba(255,255,255,0.8)' },
  feuille: {
    flex: 1,
    marginTop: -28,
    backgroundColor: couleurs.fond,
    borderTopLeftRadius: rayon.xl,
    borderTopRightRadius: rayon.xl,
    padding: espace.xl,
    gap: espace.xl,
  },
  cases: { flexDirection: 'row', justifyContent: 'space-between', gap: espace.s },
  case: {
    flex: 1,
    aspectRatio: 0.85,
    maxWidth: 56,
    borderRadius: rayon.m,
    borderWidth: 1.5,
    borderColor: couleurs.bord,
    backgroundColor: couleurs.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chiffre: { fontSize: 24, fontWeight: '700', color: couleurs.texte },
  champInvisible: { position: 'absolute', opacity: 0, width: 1, height: 1 },
  liens: { flexDirection: 'row', justifyContent: 'space-between' },
  lien: { fontSize: 15, fontWeight: '600', color: couleurs.marque },
  securite: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 6 },
});
