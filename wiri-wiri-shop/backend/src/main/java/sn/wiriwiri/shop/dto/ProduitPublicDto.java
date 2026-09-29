package sn.wiriwiri.shop.dto;

import java.util.UUID;
import sn.wiriwiri.shop.entity.Produit;

public record ProduitPublicDto(UUID id, String nomProduit, Integer prix, String taille, String imageUrl) {

    public static ProduitPublicDto de(Produit p) {
        return new ProduitPublicDto(p.getId(), p.getNomProduit(), p.getPrix(), p.getTaille(), p.getImageUrl());
    }
}
