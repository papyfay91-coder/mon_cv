import type { AudioSource } from 'expo-audio';

/**
 * Enregistrements des conditions d'utilisation du micro, lus en wolof par une vraie voix
 * (aucune synthèse vocale ne parle wolof). Une version par lieu de traitement, car le texte diffère.
 *
 * Pour les ajouter : suivre assets/audio/LISEZMOI.md, puis décommenter les lignes ci-dessous.
 * Tant qu'une version manque, le bouton « Écouter en wolof » n'est pas affiché pour ce mode.
 */
export const CONDITIONS_WOLOF: Partial<Record<'LOCAL' | 'EXTERNE', AudioSource>> = {
  // LOCAL: require('../../assets/audio/conditions-wo-local.m4a'),
  // EXTERNE: require('../../assets/audio/conditions-wo-externe.m4a'),
};
