package sn.wiriwiri.shop.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.wiriwiri.shop.dto.ProduitPublicDto;
import sn.wiriwiri.shop.dto.ProduitVitrineDto;
import sn.wiriwiri.shop.dto.VitrineDto;
import sn.wiriwiri.shop.entity.Boutique;
import sn.wiriwiri.shop.entity.Produit;
import sn.wiriwiri.shop.exception.ApiException;
import sn.wiriwiri.shop.repository.BoutiqueRepository;
import sn.wiriwiri.shop.repository.ProduitRepository;

/** Lecture publique des vitrines : seuls les produits et boutiques actifs sont exposés. */
@Service
public class VitrineService {

    public static final int TAILLE_PAGE = 24;

    private final BoutiqueRepository boutiques;
    private final ProduitRepository produits;

    public VitrineService(BoutiqueRepository boutiques, ProduitRepository produits) {
        this.boutiques = boutiques;
        this.produits = produits;
    }

    @Transactional(readOnly = true)
    public VitrineDto vitrine(UUID boutiqueId, int page) {
        Boutique b = boutiques.findByIdAndActifTrue(boutiqueId).orElseThrow(() -> ApiException.introuvable("Boutique"));
        Page<Produit> resultats = produits.findByBoutiqueIdAndActifTrue(boutiqueId,
                PageRequest.of(Math.max(0, page), TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "dateCreation")));
        return new VitrineDto(b.getId(), b.getNomVendeur(),
                resultats.getContent().stream().map(ProduitPublicDto::de).toList(),
                resultats.getNumber(), resultats.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProduitVitrineDto produit(UUID produitId) {
        Produit p = produits.trouverPubliable(produitId).orElseThrow(() -> ApiException.introuvable("Produit"));
        return new ProduitVitrineDto(ProduitPublicDto.de(p), p.getBoutique().getId(), p.getBoutique().getNomVendeur());
    }
}
