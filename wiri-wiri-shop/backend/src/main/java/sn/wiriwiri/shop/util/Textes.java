package sn.wiriwiri.shop.util;

import java.text.Normalizer;

/**
 * Nettoyage défensif des textes entrants (défense en profondeur contre le XSS stocké,
 * en complément de l'échappement automatique de React) : suppression des balises,
 * des caractères de contrôle et des espaces superflus.
 */
public final class Textes {

    private Textes() {
    }

    public static String nettoyer(String brut, int longueurMax) {
        if (brut == null) {
            return null;
        }
        String t = Normalizer.normalize(brut, Normalizer.Form.NFC)
                .replaceAll("<[^>]*>", "")
                .replaceAll("[<>]", "")
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (t.length() > longueurMax) {
            t = t.substring(0, longueurMax).trim();
        }
        return t.isEmpty() ? null : t;
    }
}
