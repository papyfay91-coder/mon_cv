package sn.wiriwiri.shop.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import sn.wiriwiri.shop.entity.Produit;

public record ProduitDto(
        UUID id,
        String nomProduit,
        Integer prix,
        String taille,
        String imageUrl,
        boolean actif,
        LocalDateTime dateCreation) {

    public static ProduitDto de(Produit p) {
        return new ProduitDto(p.getId(), p.getNomProduit(), p.getPrix(), p.getTaille(), p.getImageUrl(),
                p.isActif(), p.getDateCreation());
    }
}
