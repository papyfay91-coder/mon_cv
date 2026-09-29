package sn.wiriwiri.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Champs validés par le vendeur après pré-remplissage par l'IA (l'image arrive en multipart à part). */
public record CreationProduit(
        @NotBlank @Size(max = 150) String nomProduit,
        @NotNull @Positive @Max(100_000_000) Integer prix,
        @Size(max = 20) String taille) {
}
