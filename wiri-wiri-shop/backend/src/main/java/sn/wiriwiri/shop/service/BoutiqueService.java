package sn.wiriwiri.shop.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.wiriwiri.shop.config.WiriWiriProperties;
import sn.wiriwiri.shop.dto.BoutiqueDto;
import sn.wiriwiri.shop.dto.MiseAJourBoutique;
import sn.wiriwiri.shop.entity.Boutique;
import sn.wiriwiri.shop.exception.ApiException;
import sn.wiriwiri.shop.repository.BoutiqueRepository;
import sn.wiriwiri.shop.repository.CodeOtpRepository;
import sn.wiriwiri.shop.repository.CommandeRepository;
import sn.wiriwiri.shop.repository.ProduitRepository;
import sn.wiriwiri.shop.util.Textes;

@Service
public class BoutiqueService {

    private static final Logger LOG = LoggerFactory.getLogger(BoutiqueService.class);

    private final BoutiqueRepository boutiques;
    private final ProduitRepository produits;
    private final CommandeRepository commandes;
    private final CodeOtpRepository codes;
    private final StockageService stockage;
    private final Clock horloge;
    private final BoutiqueDto.TraitementVocal traitementVocal;

    public BoutiqueService(BoutiqueRepository boutiques, ProduitRepository produits, CommandeRepository commandes,
                           CodeOtpRepository codes, StockageService stockage, Clock horloge,
                           WiriWiriProperties proprietes) {
        this.boutiques = boutiques;
        this.produits = produits;
        this.commandes = commandes;
        this.codes = codes;
        this.stockage = stockage;
        this.horloge = horloge;
        this.traitementVocal = BoutiqueDto.TraitementVocal.de(proprietes.ia());
    }

    private BoutiqueDto dto(Boutique b) {
        return BoutiqueDto.de(b, traitementVocal);
    }

    Boutique charger(UUID id) {
        // Un jeton encore valide après suppression du compte ne donne accès à rien.
        return boutiques.findById(id).orElseThrow(ApiException::nonAuthentifie);
    }

    @Transactional(readOnly = true)
    public BoutiqueDto profil(UUID id) {
        return dto(charger(id));
    }

    @Transactional
    public BoutiqueDto mettreAJour(UUID id, MiseAJourBoutique maj) {
        Boutique b = charger(id);
        if (maj.nomVendeur() != null) {
            String nom = Textes.nettoyer(maj.nomVendeur(), 100);
            if (nom == null || nom.length() < 2) {
                throw ApiException.invalide("NOM_INVALIDE", "Nom de boutique invalide.");
            }
            b.setNomVendeur(nom);
        }
        if (maj.lienWave() != null) {
            b.setLienWave(maj.lienWave().isBlank() ? null : maj.lienWave().trim());
        }
        return dto(b);
    }

    /** Consentement explicite (CDP) au traitement de la voix, horodaté. */
    @Transactional
    public BoutiqueDto consentir(UUID id) {
        Boutique b = charger(id);
        if (!b.aConsenti()) {
            b.setDateConsentement(LocalDateTime.now(horloge));
        }
        return dto(b);
    }

    @Transactional
    public BoutiqueDto retirerConsentement(UUID id) {
        Boutique b = charger(id);
        b.setDateConsentement(null);
        return dto(b);
    }

    /**
     * Droit à l'oubli : suppression définitive de la boutique, de ses produits, des commandes
     * associées, du code OTP éventuel, puis des images une fois la transaction validée.
     */
    @Transactional
    public void supprimerDefinitivement(UUID id) {
        Boutique b = charger(id);
        List<String> images = produits.imagesDeLaBoutique(id);
        commandes.supprimerParBoutique(id);
        produits.supprimerParBoutique(id);
        codes.findById(b.getTelephone()).ifPresent(codes::delete);
        boutiques.delete(b);
        ApresTransaction.siValidee(() -> images.forEach(stockage::supprimer));
        LOG.info("Compte {} supprimé définitivement ({} produit(s))", id, images.size());
    }
}
