import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import FormulaireCommande from '@/components/FormulaireCommande';
import { chargerProduit } from '@/lib/api';
import { formaterPrix } from '@/lib/nettoyage';

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const donnees = await chargerProduit(id);
  if (!donnees) return { title: 'Article introuvable' };
  const { produit, nomVendeur } = donnees;
  return {
    title: `${produit.nomProduit} · ${nomVendeur}`,
    description: `${produit.nomProduit} à ${formaterPrix(produit.prix)} chez ${nomVendeur}`,
    openGraph: { images: [produit.imageUrl] },
  };
}

export default async function PageProduit({ params }: Props) {
  const { id } = await params;
  const donnees = await chargerProduit(id);
  if (!donnees) notFound();
  const { produit, boutiqueId, nomVendeur } = donnees;

  return (
    <main className="page fiche">
      <Link href={`/boutique/${boutiqueId}`} className="marque">← {nomVendeur}</Link>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={produit.imageUrl} alt={produit.nomProduit} width={480} height={480} decoding="async" />
      <h1>{produit.nomProduit}</h1>
      <span className="prix">{formaterPrix(produit.prix)}</span>
      {produit.taille && <span className="taille"> · Taille {produit.taille}</span>}

      <FormulaireCommande produitId={produit.id} />
    </main>
  );
}
