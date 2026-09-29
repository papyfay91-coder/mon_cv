package sn.wiriwiri.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Mise à jour partielle : seuls les champs non nuls sont appliqués. */
public record MiseAJourProduit(
        @Size(min = 1, max = 150) String nomProduit,
        @Positive @Max(100_000_000) Integer prix,
        @Size(max = 20) String taille,
        Boolean actif) {
}
