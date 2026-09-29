package sn.wiriwiri.shop.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import sn.wiriwiri.shop.entity.Commande;

public record CommandeVendeurDto(
        UUID id,
        UUID produitId,
        String nomProduit,
        Integer prix,
        String telephoneClient,
        String quartierLivraison,
        LocalDateTime dateCommande) {

    public static CommandeVendeurDto de(Commande c) {
        var p = c.getProduit();
        return new CommandeVendeurDto(c.getId(), p.getId(), p.getNomProduit(), p.getPrix(), c.getTelephoneClient(),
                c.getQuartierLivraison(), c.getDateCommande());
    }
}
