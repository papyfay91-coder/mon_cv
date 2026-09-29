package sn.wiriwiri.shop.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.wiriwiri.shop.entity.Produit;

public interface ProduitRepository extends JpaRepository<Produit, UUID> {

    /** Vitrine client : s'appuie sur l'index composite (boutique_id, actif). */
    Page<Produit> findByBoutiqueIdAndActifTrue(UUID boutiqueId, Pageable pageable);

    List<Produit> findByBoutiqueIdOrderByDateCreationDesc(UUID boutiqueId);

    Optional<Produit> findByIdAndBoutiqueId(UUID id, UUID boutiqueId);

    @Query("select p from Produit p join fetch p.boutique b where p.id = :id and p.actif = true and b.actif = true")
    Optional<Produit> trouverPubliable(@Param("id") UUID id);

    @Query("select p.imageUrl from Produit p where p.boutique.id = :boutiqueId")
    List<String> imagesDeLaBoutique(@Param("boutiqueId") UUID boutiqueId);

    @Modifying
    @Query("delete from Produit p where p.boutique.id = :boutiqueId")
    int supprimerParBoutique(@Param("boutiqueId") UUID boutiqueId);
}
