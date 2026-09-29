package sn.wiriwiri.shop.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.wiriwiri.shop.dto.CommandeVendeurDto;
import sn.wiriwiri.shop.securite.Vendeur;
import sn.wiriwiri.shop.service.CommandeService;

@RestController
@RequestMapping("/api/vendeur/commandes")
public class CommandeVendeurController {

    private final CommandeService commandes;

    public CommandeVendeurController(CommandeService commandes) {
        this.commandes = commandes;
    }

    @GetMapping
    public List<CommandeVendeurDto> lister() {
        return commandes.commandesDuVendeur(Vendeur.boutiqueCourante());
    }
}
