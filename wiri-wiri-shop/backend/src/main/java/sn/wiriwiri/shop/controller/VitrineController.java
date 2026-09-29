package sn.wiriwiri.shop.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.wiriwiri.shop.dto.CommandeCreeeDto;
import sn.wiriwiri.shop.dto.CreationCommande;
import sn.wiriwiri.shop.dto.ProduitVitrineDto;
import sn.wiriwiri.shop.dto.VitrineDto;
import sn.wiriwiri.shop.service.CommandeService;
import sn.wiriwiri.shop.service.VitrineService;

/** API publique consommée par la vitrine Next.js (aucune authentification). */
@RestController
@RequestMapping("/api/vitrine")
public class VitrineController {

    /** Court cache HTTP partagé : allège les réseaux 3G/4G et le serveur. */
    private static final CacheControl CACHE = CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic();

    private final VitrineService vitrine;
    private final CommandeService commandes;

    public VitrineController(VitrineService vitrine, CommandeService commandes) {
        this.vitrine = vitrine;
        this.commandes = commandes;
    }

    @GetMapping("/boutiques/{id}")
    public ResponseEntity<VitrineDto> boutique(@PathVariable UUID id,
                                               @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(CACHE).body(vitrine.vitrine(id, page));
    }

    @GetMapping("/produits/{id}")
    public ResponseEntity<ProduitVitrineDto> produit(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CACHE).body(vitrine.produit(id));
    }

    /** Intention d'achat d'un client : renvoie les liens de paiement direct vers le vendeur. */
    @PostMapping("/commandes")
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeCreeeDto commander(@Valid @RequestBody CreationCommande demande) {
        return commandes.creer(demande);
    }
}
