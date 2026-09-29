package sn.wiriwiri.shop.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.wiriwiri.shop.entity.Boutique;

public interface BoutiqueRepository extends JpaRepository<Boutique, UUID> {

    Optional<Boutique> findByTelephone(String telephone);

    Optional<Boutique> findByIdAndActifTrue(UUID id);
}
