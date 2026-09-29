export interface ProduitPublic {
  id: string;
  nomProduit: string;
  prix: number;
  taille: string | null;
  imageUrl: string;
}

export interface Vitrine {
  boutiqueId: string;
  nomVendeur: string;
  produits: ProduitPublic[];
  page: number;
  totalPages: number;
}

export interface ProduitVitrine {
  produit: ProduitPublic;
  boutiqueId: string;
  nomVendeur: string;
}

export interface LiensPaiement {
  whatsapp: string;
  wave: string | null;
  orangeMoney: string;
  numeroVendeur: string;
}

export interface CommandeCreee {
  commandeId: string;
  nomProduit: string;
  montant: number;
  paiement: LiensPaiement;
}

export interface Probleme {
  status: number;
  code?: string;
  detail?: string;
  champs?: Record<string, string>;
}
