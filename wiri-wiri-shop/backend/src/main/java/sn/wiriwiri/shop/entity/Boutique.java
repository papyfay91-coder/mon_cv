package sn.wiriwiri.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "boutiques")
public class Boutique {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @Column(name = "nom_vendeur", nullable = false, length = 100)
    private String nomVendeur;

    @Column(name = "telephone", nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Date du consentement explicite (CDP) à l'enregistrement et au traitement vocal. */
    @Column(name = "date_consentement")
    private LocalDateTime dateConsentement;

    /** Lien de paiement Wave du vendeur (le SaaS ne détient jamais de fonds). */
    @Column(name = "lien_wave", length = 512)
    private String lienWave;

    protected Boutique() {
    }

    public Boutique(String nomVendeur, String telephone, LocalDateTime dateCreation) {
        this.nomVendeur = nomVendeur;
        this.telephone = telephone;
        this.dateCreation = dateCreation;
    }

    public UUID getId() {
        return id;
    }

    public String getNomVendeur() {
        return nomVendeur;
    }

    public void setNomVendeur(String nomVendeur) {
        this.nomVendeur = nomVendeur;
    }

    public String getTelephone() {
        return telephone;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDateConsentement() {
        return dateConsentement;
    }

    public void setDateConsentement(LocalDateTime dateConsentement) {
        this.dateConsentement = dateConsentement;
    }

    public boolean aConsenti() {
        return dateConsentement != null;
    }

    public String getLienWave() {
        return lienWave;
    }

    public void setLienWave(String lienWave) {
        this.lienWave = lienWave;
    }
}
