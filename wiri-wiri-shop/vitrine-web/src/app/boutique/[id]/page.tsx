import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { chargerVitrine } from '@/lib/api';
import { formaterPrix } from '@/lib/nettoyage';

type Props = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ page?: string }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const vitrine = await chargerVitrine(id);
  return { title: vitrine ? `${vitrine.nomVendeur} · Wiri-Wiri Shop` : 'Boutique introuvable' };
}

export default async function PageBoutique({ params, searchParams }: Props) {
  const { id } = await params;
  const page = Number((await searchParams).page ?? 0) || 0;
  const vitrine = await chargerVitrine(id, page);
  if (!vitrine) notFound();

  return (
    <main className="page">
      <header className="entete">
        <h1>{vitrine.nomVendeur}</h1>
        <span className="marque">Wiri-Wiri Shop</span>
      </header>

      {vitrine.produits.length === 0 ? (
        <p>Aucun article pour le moment.</p>
      ) : (
        <div className="grille">
          {vitrine.produits.map((p, i) => (
            <Link key={p.id} href={`/produit/${p.id}`} className="carte">
              {/* Balise img native : pas d'optimiseur serveur, chargement différé sauf au-dessus de la ligne de flottaison. */}
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={p.imageUrl} alt={p.nomProduit} width={300} height={300}
                   loading={i < 4 ? 'eager' : 'lazy'} decoding="async" />
              <div className="infos">
                <p className="nom">{p.nomProduit}</p>
                <span className="prix">{formaterPrix(p.prix)}</span>
                {p.taille && <span className="taille"> · Taille {p.taille}</span>}
              </div>
            </Link>
          ))}
        </div>
      )}

      {vitrine.totalPages > 1 && (
        <nav className="pagination">
          {vitrine.page > 0 ? <Link href={`?page=${vitrine.page - 1}`}>← Précédent</Link> : <span />}
          {vitrine.page + 1 < vitrine.totalPages && <Link href={`?page=${vitrine.page + 1}`}>Suivant →</Link>}
        </nav>
      )}
    </main>
  );
}
