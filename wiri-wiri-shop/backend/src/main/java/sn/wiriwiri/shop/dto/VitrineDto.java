package sn.wiriwiri.shop.dto;

import java.util.List;
import java.util.UUID;

public record VitrineDto(
        UUID boutiqueId,
        String nomVendeur,
        List<ProduitPublicDto> produits,
        int page,
        int totalPages) {
}
