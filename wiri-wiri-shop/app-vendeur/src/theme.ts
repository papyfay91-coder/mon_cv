import { StyleSheet } from 'react-native';

export const couleurs = {
  fond: '#fffaf3',
  texte: '#1d1b16',
  doux: '#6b645a',
  carte: '#ffffff',
  bord: '#eadfce',
  accent: '#0e7c5a',
  danger: '#b3261e',
  enregistrement: '#d93025',
};

export const styles = StyleSheet.create({
  ecran: { flex: 1, backgroundColor: couleurs.fond, padding: 20, gap: 16 },
  titre: { fontSize: 26, fontWeight: '700', color: couleurs.texte },
  texte: { fontSize: 17, color: couleurs.texte, lineHeight: 24 },
  doux: { fontSize: 14, color: couleurs.doux },
  champ: {
    fontSize: 20,
    padding: 16,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: couleurs.bord,
    backgroundColor: couleurs.carte,
    color: couleurs.texte,
  },
  bouton: {
    backgroundColor: couleurs.accent,
    padding: 18,
    borderRadius: 14,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 10,
  },
  boutonTexte: { color: '#fff', fontSize: 19, fontWeight: '700' },
  boutonSecondaire: {
    padding: 16,
    borderRadius: 14,
    borderWidth: 2,
    borderColor: couleurs.accent,
    alignItems: 'center',
  },
  boutonSecondaireTexte: { color: couleurs.accent, fontSize: 17, fontWeight: '600' },
  erreur: { color: couleurs.danger, fontSize: 15 },
});
