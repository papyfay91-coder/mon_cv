package sn.wiriwiri.shop.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.wiriwiri.shop.dto.BoutiqueDto;
import sn.wiriwiri.shop.dto.MiseAJourBoutique;
import sn.wiriwiri.shop.securite.Vendeur;
import sn.wiriwiri.shop.service.BoutiqueService;

@RestController
@RequestMapping("/api/vendeur")
public class BoutiqueController {

    private final BoutiqueService boutiques;

    public BoutiqueController(BoutiqueService boutiques) {
        this.boutiques = boutiques;
    }

    @GetMapping("/boutique")
    public BoutiqueDto profil() {
        return boutiques.profil(Vendeur.boutiqueCourante());
    }

    @PutMapping("/boutique")
    public BoutiqueDto mettreAJour(@Valid @RequestBody MiseAJourBoutique maj) {
        return boutiques.mettreAJour(Vendeur.boutiqueCourante(), maj);
    }

    /** Consentement explicite au traitement vocal, donné après lecture des conditions (CDP). */
    @PostMapping("/consentement")
    public BoutiqueDto consentir() {
        return boutiques.consentir(Vendeur.boutiqueCourante());
    }

    @DeleteMapping("/consentement")
    public BoutiqueDto retirerConsentement() {
        return boutiques.retirerConsentement(Vendeur.boutiqueCourante());
    }

    /** Droit à l'oubli : suppression définitive du compte et de toutes ses données. */
    @DeleteMapping("/compte")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerCompte() {
        boutiques.supprimerDefinitivement(Vendeur.boutiqueCourante());
    }
}
