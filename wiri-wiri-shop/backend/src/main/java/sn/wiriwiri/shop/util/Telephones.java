package sn.wiriwiri.shop.util;

import java.util.regex.Pattern;
import sn.wiriwiri.shop.exception.ApiException;

/** Normalisation des numéros sénégalais au format E.164 (+221XXXXXXXXX). */
public final class Telephones {

    /** Mobiles (70, 71, 75, 76, 77, 78) et fixes (33) du plan de numérotation sénégalais. */
    private static final Pattern SENEGAL = Pattern.compile("^\\+221(7[015678]|33)\\d{7}$");

    private Telephones() {
    }

    public static String normaliser(String brut) {
        if (brut == null) {
            throw ApiException.invalide("TELEPHONE_INVALIDE", "Numéro de téléphone requis.");
        }
        String n = brut.replaceAll("[\\s.\\-()]", "");
        if (n.startsWith("00221")) {
            n = "+" + n.substring(2);
        } else if (n.startsWith("221") && n.length() == 12) {
            n = "+" + n;
        } else if (n.length() == 9 && !n.startsWith("+")) {
            n = "+221" + n;
        }
        if (!SENEGAL.matcher(n).matches()) {
            throw ApiException.invalide("TELEPHONE_INVALIDE", "Numéro de téléphone sénégalais invalide.");
        }
        return n;
    }

    /** Pour les journaux : +221*******12. */
    public static String masquer(String telephone) {
        if (telephone == null || telephone.length() < 6) {
            return "***";
        }
        return telephone.substring(0, 4) + "*".repeat(telephone.length() - 6) + telephone.substring(telephone.length() - 2);
    }
}
