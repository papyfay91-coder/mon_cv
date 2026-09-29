package sn.wiriwiri.shop.dto;

import java.util.UUID;

public record CommandeCreeeDto(UUID commandeId, String nomProduit, Integer montant, LiensPaiement paiement) {
}
