package sn.wiriwiri.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "produits")
public class Produit {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boutique_id", nullable = false, updatable = false)
    private Boutique boutique;

    @Column(name = "nom_produit", nullable = false, length = 150)
    private String nomProduit;

    @Column(name = "prix", nullable = false)
    private Integer prix;

    @Column(name = "taille", length = 20)
    private String taille;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    /** Reste null : l'audio source est supprimé après extraction (principe de finalité CDP). */
    @Column(name = "audio_url", length = 512)
    private String audioUrl;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    protected Produit() {
    }

    public Produit(Boutique boutique, String nomProduit, Integer prix, String taille, String imageUrl,
                   LocalDateTime dateCreation) {
        this.boutique = boutique;
        this.nomProduit = nomProduit;
        this.prix = prix;
        this.taille = taille;
        this.imageUrl = imageUrl;
        this.dateCreation = dateCreation;
    }

    public UUID getId() {
        return id;
    }

    public Boutique getBoutique() {
        return boutique;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    public Integer getPrix() {
        return prix;
    }

    public void setPrix(Integer prix) {
        this.prix = prix;
    }

    public String getTaille() {
        return taille;
    }

    public void setTaille(String taille) {
        this.taille = taille;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
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
}
