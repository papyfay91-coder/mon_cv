package sn.wiriwiri.shop.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.wiriwiri.shop.config.WiriWiriProperties;

/**
 * Stockage des images produits. Implémentation sur disque local ; peut être remplacée
 * par un stockage objet (S3, R2…) sans toucher aux autres couches.
 * Les noms de fichiers sont générés par le serveur (UUID) : aucune traversée de répertoire possible.
 */
@Service
public class StockageService {

    private static final Logger LOG = LoggerFactory.getLogger(StockageService.class);
    private static final Pattern NOM_VALIDE = Pattern.compile("^[0-9a-f\\-]{36}\\.(jpg|png|webp)$");

    private final Path racine;
    private final String urlPublique;

    public StockageService(WiriWiriProperties proprietes) {
        this.racine = Path.of(proprietes.stockage().repertoire()).toAbsolutePath().normalize();
        String url = proprietes.stockage().urlPublique();
        this.urlPublique = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        try {
            Files.createDirectories(racine);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le répertoire de stockage " + racine, e);
        }
    }

    public Path racine() {
        return racine;
    }

    public String enregistrer(byte[] octets, TypeFichier type) {
        String nom = UUID.randomUUID() + "." + type.extension();
        try {
            Files.write(racine.resolve(nom), octets, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'enregistrement du fichier", e);
        }
        return urlPublique + "/" + nom;
    }

    public void supprimer(String url) {
        if (url == null || !url.startsWith(urlPublique + "/")) {
            return;
        }
        String nom = url.substring(urlPublique.length() + 1);
        if (!NOM_VALIDE.matcher(nom).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(racine.resolve(nom));
        } catch (IOException e) {
            LOG.warn("Suppression impossible du fichier {}", nom, e);
        }
    }
}
