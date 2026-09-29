package sn.wiriwiri.shop.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MiseAJourBoutique(
        @Size(min = 2, max = 100) String nomVendeur,
        @Size(max = 512)
        @Pattern(regexp = "^(https://pay\\.wave\\.com/[A-Za-z0-9/_\\-?=&.]+)?$",
                message = "Le lien doit être un lien de paiement https://pay.wave.com/…")
        String lienWave) {
}
