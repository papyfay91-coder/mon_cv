package sn.wiriwiri.shop.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.wiriwiri.shop.entity.Commande;

public interface CommandeRepository extends JpaRepository<Commande, UUID> {

    @Query("select c from Commande c join fetch c.produit p where p.boutique.id = :boutiqueId order by c.dateCommande desc")
    List<Commande> commandesDeLaBoutique(@Param("boutiqueId") UUID boutiqueId);

    @Modifying
    @Query("delete from Commande c where c.produit.id in (select p.id from Produit p where p.boutique.id = :boutiqueId)")
    int supprimerParBoutique(@Param("boutiqueId") UUID boutiqueId);

    @Modifying
    @Query("delete from Commande c where c.produit.id = :produitId")
    int supprimerParProduit(@Param("produitId") UUID produitId);
}
