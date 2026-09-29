import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

export const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://10.0.2.2:8080';
export const VITRINE_URL = process.env.EXPO_PUBLIC_VITRINE_URL ?? 'http://localhost:3000';

const CLE_JETON = 'wiriwiri.jeton';

export interface Boutique {
  id: string;
  nomVendeur: string;
  telephone: string;
  consentementDonne: boolean;
  lienWave: string | null;
}

export interface Produit {
  id: string;
  nomProduit: string;
  prix: number;
  taille: string | null;
  imageUrl: string;
  actif: boolean;
}

export interface Extraction {
  transcription: string;
  nomProduit: string | null;
  prix: number | null;
  taille: string | null;
  complet: boolean;
}

export class ErreurApi extends Error {
  constructor(public status: number, public code: string | undefined, message: string) {
    super(message);
  }
}

/** Le JWT est conservé dans le trousseau chiffré du téléphone (Keychain / Keystore), jamais en clair. */
export const jeton = {
  lire: () => SecureStore.getItemAsync(CLE_JETON),
  ecrire: (valeur: string) => SecureStore.setItemAsync(CLE_JETON, valeur),
  effacer: () => SecureStore.deleteItemAsync(CLE_JETON),
};

async function requete<T>(chemin: string, options: RequestInit = {}): Promise<T> {
  const valeur = await jeton.lire();
  const entetes: Record<string, string> = { ...(options.headers as Record<string, string>) };
  if (valeur) entetes.Authorization = `Bearer ${valeur}`;
  if (options.body && !(options.body instanceof FormData)) entetes['Content-Type'] = 'application/json';

  const reponse = await fetch(`${API_URL}${chemin}`, { ...options, headers: entetes });
  if (reponse.status === 204) return undefined as T;
  const corps = await reponse.json().catch(() => ({}));
  if (!reponse.ok) {
    throw new ErreurApi(reponse.status, corps.code, corps.detail ?? 'Erreur réseau');
  }
  return corps as T;
}

/**
 * Pièce jointe multipart : React Native (iOS / Android) attend { uri, name, type } ;
 * le navigateur (aperçu web) attend un vrai Blob.
 */
async function joindre(donnees: FormData, champ: string, uri: string, name: string, type: string) {
  if (Platform.OS === 'web') {
    donnees.append(champ, await (await fetch(uri)).blob(), name);
  } else {
    donnees.append(champ, { uri, name, type } as unknown as Blob);
  }
}

export const api = {
  demanderCode: (telephone: string) =>
    requete<void>('/api/auth/otp', { method: 'POST', body: JSON.stringify({ telephone }) }),

  verifierCode: (telephone: string, code: string, nomVendeur?: string) =>
    requete<{ jeton: string; nouveauCompte: boolean }>('/api/auth/verification', {
      method: 'POST',
      body: JSON.stringify({ telephone, code, nomVendeur }),
    }),

  profil: () => requete<Boutique>('/api/vendeur/boutique'),
  mettreAJour: (maj: { nomVendeur?: string; lienWave?: string }) =>
    requete<Boutique>('/api/vendeur/boutique', { method: 'PUT', body: JSON.stringify(maj) }),
  consentir: () => requete<Boutique>('/api/vendeur/consentement', { method: 'POST' }),
  retirerConsentement: () => requete<Boutique>('/api/vendeur/consentement', { method: 'DELETE' }),
  supprimerCompte: () => requete<void>('/api/vendeur/compte', { method: 'DELETE' }),

  produits: () => requete<Produit[]>('/api/vendeur/produits'),

  analyserNoteVocale: async (uriAudio: string) => {
    const donnees = new FormData();
    const wav = uriAudio.toLowerCase().endsWith('.wav');
    await joindre(donnees, 'audio', uriAudio, wav ? 'note.wav' : 'note.m4a', wav ? 'audio/wav' : 'audio/mp4');
    return requete<Extraction>('/api/vendeur/produits/analyse-vocale', { method: 'POST', body: donnees });
  },

  publierProduit: async (p: { nomProduit: string; prix: number; taille?: string; uriImage: string; typeImage?: string }) => {
    const donnees = new FormData();
    donnees.append('nomProduit', p.nomProduit);
    donnees.append('prix', String(p.prix));
    if (p.taille) donnees.append('taille', p.taille);
    const type = p.typeImage ?? 'image/jpeg';
    await joindre(donnees, 'image', p.uriImage, `photo.${type.split('/')[1] ?? 'jpg'}`, type);
    return requete<Produit>('/api/vendeur/produits', { method: 'POST', body: donnees });
  },

  basculerProduit: (id: string, actif: boolean) =>
    requete<Produit>(`/api/vendeur/produits/${id}`, { method: 'PATCH', body: JSON.stringify({ actif }) }),

  supprimerProduit: (id: string) => requete<void>(`/api/vendeur/produits/${id}`, { method: 'DELETE' }),
};

export const formaterPrix = (fcfa: number) => `${String(fcfa).replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} FCFA`;
