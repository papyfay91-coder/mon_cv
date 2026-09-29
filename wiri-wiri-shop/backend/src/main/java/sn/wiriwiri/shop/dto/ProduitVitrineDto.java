package sn.wiriwiri.shop.dto;

import java.util.UUID;

public record ProduitVitrineDto(ProduitPublicDto produit, UUID boutiqueId, String nomVendeur) {
}
