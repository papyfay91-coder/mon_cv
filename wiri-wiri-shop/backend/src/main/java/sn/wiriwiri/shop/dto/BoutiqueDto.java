package sn.wiriwiri.shop.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import sn.wiriwiri.shop.config.WiriWiriProperties;
import sn.wiriwiri.shop.entity.Boutique;

public record BoutiqueDto(
        UUID id,
        String nomVendeur,
        String telephone,
        boolean actif,
        LocalDateTime dateCreation,
        boolean consentementDonne,
        LocalDateTime dateConsentement,
        String lienWave,
        TraitementVocal traitementVocal) {

    /** Informe le vendeur, avant son consentement, du lieu de traitement de sa voix. */
    public record TraitementVocal(WiriWiriProperties.Hebergement hebergement, String fournisseur) {

        public static TraitementVocal de(WiriWiriProperties.Ia ia) {
            String fournisseur = ia.fournisseur() == null || ia.fournisseur().isBlank() ? null : ia.fournisseur();
            return new TraitementVocal(ia.hebergement(), fournisseur);
        }
    }

    public static BoutiqueDto de(Boutique b, TraitementVocal traitement) {
        return new BoutiqueDto(b.getId(), b.getNomVendeur(), b.getTelephone(), b.isActif(), b.getDateCreation(),
                b.aConsenti(), b.getDateConsentement(), b.getLienWave(), traitement);
    }
}
