package sn.wiriwiri.shop.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sn.wiriwiri.shop.dto.CreationProduit;
import sn.wiriwiri.shop.dto.ExtractionProduitDto;
import sn.wiriwiri.shop.dto.MiseAJourProduit;
import sn.wiriwiri.shop.dto.ProduitDto;
import sn.wiriwiri.shop.securite.Vendeur;
import sn.wiriwiri.shop.service.IngestionVocaleService;
import sn.wiriwiri.shop.service.ProduitService;

@RestController
@RequestMapping("/api/vendeur/produits")
public class ProduitVendeurController {

    private final ProduitService produits;
    private final IngestionVocaleService ingestion;

    public ProduitVendeurController(ProduitService produits, IngestionVocaleService ingestion) {
        this.produits = produits;
        this.ingestion = ingestion;
    }

    /** Étape 1 : note vocale (.wav / .m4a) → proposition de fiche produit pré-remplie. */
    @PostMapping(path = "/analyse-vocale", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractionProduitDto analyserNoteVocale(@RequestPart("audio") MultipartFile audio) {
        return ingestion.analyser(Vendeur.boutiqueCourante(), audio);
    }

    /** Étape 2 : le vendeur valide les champs et publie avec la photo. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProduitDto creer(@Valid @ModelAttribute CreationProduit produit,
                            @RequestPart("image") MultipartFile image) {
        return produits.creer(Vendeur.boutiqueCourante(), produit, image);
    }

    @GetMapping
    public List<ProduitDto> lister() {
        return produits.lister(Vendeur.boutiqueCourante());
    }

    @PatchMapping("/{id}")
    public ProduitDto mettreAJour(@PathVariable UUID id, @Valid @RequestBody MiseAJourProduit maj) {
        return produits.mettreAJour(Vendeur.boutiqueCourante(), id, maj);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        produits.supprimer(Vendeur.boutiqueCourante(), id);
    }
}
