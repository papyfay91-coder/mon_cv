package sn.wiriwiri.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreationCommande(
        @NotNull UUID produitId,
        @NotBlank @Size(max = 20) String telephoneClient,
        @NotBlank @Size(max = 100) String quartierLivraison) {
}
