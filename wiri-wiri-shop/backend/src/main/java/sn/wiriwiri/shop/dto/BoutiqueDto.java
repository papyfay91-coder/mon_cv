package sn.wiriwiri.shop.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import sn.wiriwiri.shop.entity.Boutique;

public record BoutiqueDto(
        UUID id,
        String nomVendeur,
        String telephone,
        boolean actif,
        LocalDateTime dateCreation,
        boolean consentementDonne,
        LocalDateTime dateConsentement,
        String lienWave) {

    public static BoutiqueDto de(Boutique b) {
        return new BoutiqueDto(b.getId(), b.getNomVendeur(), b.getTelephone(), b.isActif(), b.getDateCreation(),
                b.aConsenti(), b.getDateConsentement(), b.getLienWave());
    }
}
