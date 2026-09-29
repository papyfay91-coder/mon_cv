package sn.wiriwiri.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import sn.wiriwiri.shop.securite.ConvertisseurChiffre;

@Entity
@Table(name = "commandes")
public class Commande {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_id", nullable = false, updatable = false)
    private Produit produit;

    /** Donnée personnelle du client : chiffrée en AES-256-GCM dans la base. */
    @Convert(converter = ConvertisseurChiffre.class)
    @Column(name = "telephone_client", nullable = false, length = 255)
    private String telephoneClient;

    @Column(name = "quartier_livraison", nullable = false, length = 100)
    private String quartierLivraison;

    @Column(name = "date_commande", nullable = false, updatable = false)
    private LocalDateTime dateCommande;

    protected Commande() {
    }

    public Commande(Produit produit, String telephoneClient, String quartierLivraison, LocalDateTime dateCommande) {
        this.produit = produit;
        this.telephoneClient = telephoneClient;
        this.quartierLivraison = quartierLivraison;
        this.dateCommande = dateCommande;
    }

    public UUID getId() {
        return id;
    }

    public Produit getProduit() {
        return produit;
    }

    public String getTelephoneClient() {
        return telephoneClient;
    }

    public String getQuartierLivraison() {
        return quartierLivraison;
    }

    public LocalDateTime getDateCommande() {
        return dateCommande;
    }
}
