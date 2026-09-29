package sn.wiriwiri.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Code à usage unique en attente de validation. Seul son condensat HMAC est conservé. */
@Entity
@Table(name = "codes_otp")
public class CodeOtp {

    @Id
    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "tentatives", nullable = false)
    private int tentatives;

    protected CodeOtp() {
    }

    public CodeOtp(String telephone, String codeHash, LocalDateTime dateExpiration) {
        this.telephone = telephone;
        this.codeHash = codeHash;
        this.dateExpiration = dateExpiration;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void remplacer(String codeHash, LocalDateTime dateExpiration) {
        this.codeHash = codeHash;
        this.dateExpiration = dateExpiration;
        this.tentatives = 0;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public int getTentatives() {
        return tentatives;
    }

    public void incrementerTentatives() {
        this.tentatives++;
    }
}
