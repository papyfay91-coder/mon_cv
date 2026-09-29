package sn.wiriwiri.shop.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.wiriwiri.shop.dto.CreationProduit;
import sn.wiriwiri.shop.dto.MiseAJourProduit;
import sn.wiriwiri.shop.dto.ProduitDto;
import sn.wiriwiri.shop.entity.Boutique;
import sn.wiriwiri.shop.entity.Produit;
import sn.wiriwiri.shop.exception.ApiException;
import sn.wiriwiri.shop.repository.ProduitRepository;
import sn.wiriwiri.shop.util.Textes;

@Service
public class ProduitService {

    static final long TAILLE_MAX_IMAGE = 5L * 1024 * 1024;

    private final ProduitRepository produits;
    private final BoutiqueService boutiques;
    private final StockageService stockage;
    private final Clock horloge;

    public ProduitService(ProduitRepository produits, BoutiqueService boutiques, StockageService stockage,
                          Clock horloge) {
        this.produits = produits;
        this.boutiques = boutiques;
        this.stockage = stockage;
        this.horloge = horloge;
    }

    @Transactional
    public ProduitDto creer(UUID boutiqueId, CreationProduit demande, MultipartFile image) {
        Boutique boutique = boutiques.charger(boutiqueId);
        String nom = Textes.nettoyer(demande.nomProduit(), 150);
        if (nom == null) {
            throw ApiException.invalide("NOM_PRODUIT_INVALIDE", "Nom de produit invalide.");
        }
        byte[] octets = lire(image);
        if (octets.length == 0 || octets.length > TAILLE_MAX_IMAGE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_INVALIDE", "Image vide ou supérieure à 5 Mo.");
        }
        TypeFichier type = TypeFichier.detecterImage(octets).orElseThrow(() -> new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_INVALIDE", "Formats acceptés : JPEG, PNG, WebP."));

        String url = stockage.enregistrer(octets, type);
        ApresTransaction.siAnnulee(() -> stockage.supprimer(url));

        Produit produit = new Produit(boutique, nom, demande.prix(), Textes.nettoyer(demande.taille(), 20), url,
                LocalDateTime.now(horloge));
        return ProduitDto.de(produits.save(produit));
    }

    @Transactional(readOnly = true)
    public List<ProduitDto> lister(UUID boutiqueId) {
        return produits.findByBoutiqueIdOrderByDateCreationDesc(boutiqueId).stream().map(ProduitDto::de).toList();
    }

    @Transactional
    public ProduitDto mettreAJour(UUID boutiqueId, UUID produitId, MiseAJourProduit maj) {
        Produit p = chargerPropre(boutiqueId, produitId);
        if (maj.nomProduit() != null) {
            String nom = Textes.nettoyer(maj.nomProduit(), 150);
            if (nom == null) {
                throw ApiException.invalide("NOM_PRODUIT_INVALIDE", "Nom de produit invalide.");
            }
            p.setNomProduit(nom);
        }
        if (maj.prix() != null) {
            p.setPrix(maj.prix());
        }
        if (maj.taille() != null) {
            p.setTaille(Textes.nettoyer(maj.taille(), 20));
        }
        if (maj.actif() != null) {
            p.setActif(maj.actif());
        }
        return ProduitDto.de(p);
    }

    @Transactional
    public void supprimer(UUID boutiqueId, UUID produitId) {
        Produit p = chargerPropre(boutiqueId, produitId);
        String image = p.getImageUrl();
        produits.delete(p);
        ApresTransaction.siValidee(() -> stockage.supprimer(image));
    }

    /** Contrôle d'appartenance : un vendeur ne peut jamais toucher au produit d'un autre (anti-IDOR). */
    private Produit chargerPropre(UUID boutiqueId, UUID produitId) {
        return produits.findByIdAndBoutiqueId(produitId, boutiqueId)
                .orElseThrow(() -> ApiException.introuvable("Produit"));
    }

    private static byte[] lire(MultipartFile fichier) {
        if (fichier == null) {
            throw ApiException.invalide("IMAGE_REQUISE", "Une photo du produit est requise.");
        }
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
