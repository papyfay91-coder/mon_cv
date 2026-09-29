import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { api, Boutique, ErreurApi, jeton } from './api';
import Connexion from './ecrans/Connexion';
import Consentement from './ecrans/Consentement';
import MesProduits from './ecrans/MesProduits';
import NouveauProduit from './ecrans/NouveauProduit';
import Parametres from './ecrans/Parametres';
import { couleurs } from './theme';

type Ecran = 'chargement' | 'connexion' | 'consentement' | 'produits' | 'nouveau' | 'parametres';

/** Navigation volontairement minimale (machine à états) : application légère, peu de dépendances. */
export default function App() {
  const [ecran, setEcran] = useState<Ecran>('chargement');
  const [boutique, setBoutique] = useState<Boutique | null>(null);

  const chargerProfil = useCallback(async () => {
    try {
      if (!(await jeton.lire())) return setEcran('connexion');
      const profil = await api.profil();
      setBoutique(profil);
      setEcran(profil.consentementDonne ? 'produits' : 'consentement');
    } catch (e) {
      if (e instanceof ErreurApi && e.status === 401) await jeton.effacer();
      setEcran('connexion');
    }
  }, []);

  useEffect(() => {
    chargerProfil();
  }, [chargerProfil]);

  const deconnecter = async () => {
    await jeton.effacer();
    setBoutique(null);
    setEcran('connexion');
  };

  let contenu: React.ReactNode;
  switch (ecran) {
    case 'chargement':
      contenu = (
        <View style={{ flex: 1, justifyContent: 'center' }}>
          <ActivityIndicator size="large" color={couleurs.accent} />
        </View>
      );
      break;
    case 'connexion':
      contenu = <Connexion onConnecte={chargerProfil} />;
      break;
    case 'consentement':
      contenu = (
        <Consentement
          onAccepte={(b) => {
            setBoutique(b);
            setEcran('produits');
          }}
          onRefuse={() => setEcran('produits')}
        />
      );
      break;
    case 'produits':
      contenu = (
        <MesProduits
          boutique={boutique!}
          onAjouter={() => setEcran('nouveau')}
          onParametres={() => setEcran('parametres')}
        />
      );
      break;
    case 'nouveau':
      contenu = (
        <NouveauProduit
          vocalAutorise={boutique?.consentementDonne ?? false}
          onDemanderConsentement={() => setEcran('consentement')}
          onTermine={() => setEcran('produits')}
        />
      );
      break;
    case 'parametres':
      contenu = (
        <Parametres
          boutique={boutique!}
          onMiseAJour={setBoutique}
          onRetour={() => setEcran('produits')}
          onDeconnexion={deconnecter}
        />
      );
      break;
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={{ flex: 1, backgroundColor: couleurs.fond }}>
        <StatusBar style="dark" />
        {contenu}
      </SafeAreaView>
    </SafeAreaProvider>
  );
}
