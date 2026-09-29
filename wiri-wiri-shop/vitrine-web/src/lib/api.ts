import type { CommandeCreee, ProduitVitrine, Probleme, Vitrine } from './types';

const PUBLIC_API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
/** Côté serveur on peut joindre l'API par le réseau interne. */
const SERVER_API = process.env.API_URL ?? PUBLIC_API;

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export const estUuid = (id: string) => UUID.test(id);

async function lirePublic<T>(chemin: string): Promise<T | null> {
  // ISR : la page est régénérée au plus toutes les 60 s, servie depuis le cache entre-temps.
  const reponse = await fetch(`${SERVER_API}${chemin}`, { next: { revalidate: 60 } });
  if (reponse.status === 404) return null;
  if (!reponse.ok) throw new Error(`API ${reponse.status}`);
  return (await reponse.json()) as T;
}

export function chargerVitrine(boutiqueId: string, page = 0) {
  if (!estUuid(boutiqueId)) return Promise.resolve(null);
  return lirePublic<Vitrine>(`/api/vitrine/boutiques/${boutiqueId}?page=${Math.max(0, page | 0)}`);
}

export function chargerProduit(produitId: string) {
  if (!estUuid(produitId)) return Promise.resolve(null);
  return lirePublic<ProduitVitrine>(`/api/vitrine/produits/${produitId}`);
}

export class ErreurApi extends Error {
  constructor(public probleme: Probleme) {
    super(probleme.detail ?? 'Erreur');
  }
}

export async function passerCommande(donnees: {
  produitId: string;
  telephoneClient: string;
  quartierLivraison: string;
}): Promise<CommandeCreee> {
  const reponse = await fetch(`${PUBLIC_API}/api/vitrine/commandes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(donnees),
  });
  if (!reponse.ok) {
    const probleme = (await reponse.json().catch(() => ({ status: reponse.status }))) as Probleme;
    throw new ErreurApi(probleme);
  }
  return (await reponse.json()) as CommandeCreee;
}
