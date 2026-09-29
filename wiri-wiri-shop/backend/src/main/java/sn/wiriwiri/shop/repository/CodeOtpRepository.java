package sn.wiriwiri.shop.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.wiriwiri.shop.entity.CodeOtp;

public interface CodeOtpRepository extends JpaRepository<CodeOtp, String> {

    /** Verrou pessimiste : deux vérifications simultanées ne peuvent pas contourner le compteur de tentatives. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodeOtp c where c.telephone = :telephone")
    Optional<CodeOtp> verrouiller(@Param("telephone") String telephone);

    @Modifying
    @Query("delete from CodeOtp c where c.dateExpiration < :limite")
    int purgerExpires(@Param("limite") LocalDateTime limite);
}
