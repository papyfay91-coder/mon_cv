import type { AudioSource } from 'expo-audio';

/**
 * Conditions d'utilisation du micro, lues en wolof. Une version par lieu de traitement, car le texte diffère.
 * Deux façons de les obtenir (voir assets/audio/LISEZMOI.md) :
 *   - voix de synthèse MMS-TTS : `python outils/generer_audio_wolof.py` (active les lignes ci-dessous) ;
 *   - enregistrement par une vraie voix (recommandé en production), puis décommenter les lignes.
 * Tant qu'une version manque, le bouton « Écouter en wolof » n'est pas affiché pour ce mode.
 */
export const CONDITIONS_WOLOF: Partial<Record<'LOCAL' | 'EXTERNE', AudioSource>> = {
  // LOCAL: require('../../assets/audio/conditions-wo-local.m4a'),
  // EXTERNE: require('../../assets/audio/conditions-wo-externe.m4a'),
};
