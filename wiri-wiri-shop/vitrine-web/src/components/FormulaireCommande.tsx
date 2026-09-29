'use client';

import { useState } from 'react';
import { ErreurApi, passerCommande } from '@/lib/api';
import { formaterPrix, lienSur, nettoyerTelephone, nettoyerTexte } from '@/lib/nettoyage';
import type { CommandeCreee } from '@/lib/types';

/**
 * Commande en deux temps : le client laisse son numéro et son quartier, puis paie
 * directement le vendeur depuis son propre compte Wave ou Orange Money.
 * Wiri-Wiri Shop ne touche jamais l'argent (conformité BCEAO).
 */
export default function FormulaireCommande({ produitId }: { produitId: string }) {
  const [telephone, setTelephone] = useState('');
  const [quartier, setQuartier] = useState('');
  const [envoi, setEnvoi] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [commande, setCommande] = useState<CommandeCreee | null>(null);

  async function soumettre(e: React.FormEvent) {
    e.preventDefault();
    setErreur(null);
    const tel = nettoyerTelephone(telephone);
    const lieu = nettoyerTexte(quartier, 100);
    if (tel.replace('+', '').length < 9) return setErreur('Numéro de téléphone invalide.');
    if (lieu.length < 2) return setErreur('Indiquez votre quartier.');

    setEnvoi(true);
    try {
      setCommande(await passerCommande({ produitId, telephoneClient: tel, quartierLivraison: lieu }));
    } catch (err) {
      setErreur(err instanceof ErreurApi && err.probleme.status === 400
        ? 'Vérifiez votre numéro (format sénégalais) et votre quartier.'
        : 'Impossible de passer la commande. Réessayez.');
    } finally {
      setEnvoi(false);
    }
  }

  if (commande) {
    const { paiement } = commande;
    const whatsapp = lienSur(paiement.whatsapp);
    const wave = lienSur(paiement.wave);
    const orange = lienSur(paiement.orangeMoney);
    return (
      <section className="paiements" aria-live="polite">
        <p><strong>Commande enregistrée !</strong> Montant : {formaterPrix(commande.montant)}</p>
        {whatsapp && (
          <a className="bouton whatsapp" href={whatsapp} target="_blank" rel="noopener noreferrer">
            1. Envoyer la commande sur WhatsApp
          </a>
        )}
        {wave && (
          <a className="bouton wave" href={wave} target="_blank" rel="noopener noreferrer">
            2. Payer avec Wave
          </a>
        )}
        {orange && (
          <a className="bouton orange" href={orange}>
            {wave ? 'ou ' : '2. '}Payer avec Orange Money (#144#)
          </a>
        )}
        <p className="note">
          Paiement direct au vendeur ({paiement.numeroVendeur}). Wiri-Wiri Shop n&apos;encaisse aucun argent.
        </p>
      </section>
    );
  }

  return (
    <form onSubmit={soumettre} noValidate>
      <label>
        Votre numéro de téléphone
        <input type="tel" inputMode="tel" autoComplete="tel" placeholder="77 123 45 67" maxLength={20}
               value={telephone} onChange={(e) => setTelephone(e.target.value)} required />
      </label>
      <label>
        Quartier de livraison
        <input type="text" autoComplete="address-level3" placeholder="Parcelles Assainies" maxLength={100}
               value={quartier} onChange={(e) => setQuartier(e.target.value)} required />
      </label>
      {erreur && <p className="erreur" role="alert">{erreur}</p>}
      <button className="bouton" type="submit" disabled={envoi}>
        {envoi ? 'Envoi…' : 'Commander'}
      </button>
    </form>
  );
}
