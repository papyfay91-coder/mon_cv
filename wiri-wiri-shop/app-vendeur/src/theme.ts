/** Jeton de design : une seule source pour les couleurs, espacements, rayons et typographies. */
export const couleurs = {
  marque: '#0B6E4F',
  marqueFonce: '#064733',
  marqueClair: '#E6F2EC',
  accent: '#F2A93B',
  accentClair: '#FDF3E1',
  fond: '#F5F6F8',
  surface: '#FFFFFF',
  bord: '#E6E8EC',
  bordFort: '#CFD4DC',
  texte: '#101828',
  texte2: '#475467',
  texte3: '#98A2B3',
  danger: '#D92D20',
  dangerClair: '#FEF3F2',
  succes: '#079455',
  succesClair: '#ECFDF3',
  enregistrement: '#E5484D',
  blanc: '#FFFFFF',
};

export const espace = { xs: 4, s: 8, m: 12, l: 16, xl: 24, xxl: 32 };

export const rayon = { s: 10, m: 14, l: 20, xl: 28, rond: 999 };

export const typo = {
  titre: { fontSize: 28, lineHeight: 34, fontWeight: '800' as const, color: couleurs.texte, letterSpacing: -0.5 },
  h2: { fontSize: 20, lineHeight: 26, fontWeight: '700' as const, color: couleurs.texte, letterSpacing: -0.2 },
  h3: { fontSize: 16, lineHeight: 22, fontWeight: '700' as const, color: couleurs.texte },
  corps: { fontSize: 16, lineHeight: 23, color: couleurs.texte2 },
  label: { fontSize: 14, lineHeight: 20, fontWeight: '600' as const, color: couleurs.texte },
  petit: { fontSize: 13, lineHeight: 18, color: couleurs.texte3 },
};

export const ombre = {
  douce: { boxShadow: '0px 2px 8px rgba(16, 24, 40, 0.06)' },
  moyenne: { boxShadow: '0px 8px 24px rgba(16, 24, 40, 0.10)' },
  forte: { boxShadow: '0px 12px 32px rgba(6, 71, 51, 0.30)' },
};
