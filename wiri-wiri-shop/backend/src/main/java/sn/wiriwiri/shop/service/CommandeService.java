package sn.wiriwiri.shop.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.wiriwiri.shop.dto.CommandeCreeeDto;
import sn.wiriwiri.shop.dto.CommandeVendeurDto;
import sn.wiriwiri.shop.dto.CreationCommande;
import sn.wiriwiri.shop.dto.LiensPaiement;
import sn.wiriwiri.shop.entity.Boutique;
import sn.wiriwiri.shop.entity.Commande;
import sn.wiriwiri.shop.entity.Produit;
import sn.wiriwiri.shop.exception.ApiException;
import sn.wiriwiri.shop.repository.CommandeRepository;
import sn.wiriwiri.shop.repository.ProduitRepository;
import sn.wiriwiri.shop.util.Telephones;
import sn.wiriwiri.shop.util.Textes;

/**
 * Enregistre l'intention d'achat et génère les liens de paiement directs vers le vendeur.
 * Conformité BCEAO : Wiri-Wiri Shop n'encaisse et ne transfère jamais d'argent.
 */
@Service
public class CommandeService {

    /** Composition du menu Orange Money (#144#) ; le « # » doit être encodé dans une URI tel:. */
    static final String USSD_ORANGE_MONEY = "tel:%23144%23";

    private final CommandeRepository commandes;
    private final ProduitRepository produits;
    private final Clock horloge;

    public CommandeService(CommandeRepository commandes, ProduitRepository produits, Clock horloge) {
        this.commandes = commandes;
        this.produits = produits;
        this.horloge = horloge;
    }

    @Transactional
    public CommandeCreeeDto creer(CreationCommande demande) {
        Produit produit = produits.trouverPubliable(demande.produitId())
                .orElseThrow(() -> ApiException.introuvable("Produit"));
        String telephoneClient = Telephones.normaliser(demande.telephoneClient());
        String quartier = Textes.nettoyer(demande.quartierLivraison(), 100);
        if (quartier == null) {
            throw ApiException.invalide("QUARTIER_REQUIS", "Indiquez le quartier de livraison.");
        }
        Commande commande = commandes.save(
                new Commande(produit, telephoneClient, quartier, LocalDateTime.now(horloge)));
        return new CommandeCreeeDto(commande.getId(), produit.getNomProduit(), produit.getPrix(),
                liens(commande, produit, produit.getBoutique()));
    }

    @Transactional(readOnly = true)
    public List<CommandeVendeurDto> commandesDuVendeur(UUID boutiqueId) {
        return commandes.commandesDeLaBoutique(boutiqueId).stream().map(CommandeVendeurDto::de).toList();
    }

    static LiensPaiement liens(Commande commande, Produit produit, Boutique boutique) {
        String numeroVendeur = boutique.getTelephone();
        String reference = commande.getId().toString().substring(0, 8).toUpperCase();
        String message = "Salam ! Nouvelle commande Wiri-Wiri Shop"
                + "\nRéf : " + reference
                + "\nArticle : " + produit.getNomProduit()
                + (produit.getTaille() != null ? " (taille " + produit.getTaille() + ")" : "")
                + "\nPrix : " + formaterMontant(produit.getPrix()) + " FCFA"
                + "\nLivraison : " + commande.getQuartierLivraison()
                + "\nMon numéro : " + commande.getTelephoneClient();
        String whatsapp = "https://wa.me/" + numeroVendeur.substring(1) + "?text="
                + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");

        String wave = null;
        if (boutique.getLienWave() != null) {
            String lien = boutique.getLienWave();
            wave = lien + (lien.contains("?") ? "&" : "?") + "amount=" + produit.getPrix();
        }
        return new LiensPaiement(whatsapp, wave, USSD_ORANGE_MONEY, numeroVendeur);
    }

    static String formaterMontant(int montant) {
        return String.format("%,d", montant).replace(',', ' ');
    }
}
