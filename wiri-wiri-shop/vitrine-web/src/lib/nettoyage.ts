/**
 * Nettoyage des saisies avant envoi (anti-XSS, défense en profondeur) :
 * React échappe déjà tout ce qu'il affiche ; on retire en plus balises et caractères de contrôle.
 */
export function nettoyerTexte(brut: string, max: number): string {
  return brut
    .normalize('NFC')
    .replace(/<[^>]*>/g, '')
    .replace(/[<>]/g, '')
    // eslint-disable-next-line no-control-regex
    .replace(/[\u0000-\u001f\u007f]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, max);
}

/** Ne garde que les chiffres et un « + » initial. */
export function nettoyerTelephone(brut: string): string {
  const t = brut.replace(/[^\d+]/g, '');
  return (t.startsWith('+') ? '+' : '') + t.replace(/\+/g, '').slice(0, 15);
}

/** N'autorise que des liens de paiement attendus (jamais de javascript:, data:, etc.). */
export function lienSur(url: string | null | undefined): string | null {
  if (!url) return null;
  if (url.startsWith('https://wa.me/') || url.startsWith('https://pay.wave.com/') || url === 'tel:%23144%23') {
    return url;
  }
  return null;
}

export const formaterPrix = (fcfa: number) =>
  `${fcfa.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} FCFA`;
