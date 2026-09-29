package sn.wiriwiri.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sn.wiriwiri.shop.dto.ExtractionProduitDto;
import sn.wiriwiri.shop.entity.Boutique;
import sn.wiriwiri.shop.exception.ApiException;
import sn.wiriwiri.shop.ia.ExtracteurLlm;
import sn.wiriwiri.shop.ia.Transcripteur;
import sn.wiriwiri.shop.util.Textes;

/**
 * Module « Voice-to-Store » (§6.1) : note vocale en Wolof → transcription Whisper →
 * extraction JSON par LLM → validation stricte des types → pré-remplissage renvoyé au vendeur.
 * <p>
 * Finalité (CDP) : l'audio n'est jamais écrit sur disque ni en base ; il n'existe qu'en mémoire
 * le temps de la requête. La sortie du LLM est traitée comme une donnée non fiable.
 */
@Service
public class IngestionVocaleService {

    static final long TAILLE_MAX_AUDIO = 10L * 1024 * 1024;

    private final BoutiqueService boutiques;
    private final Transcripteur transcripteur;
    private final ExtracteurLlm extracteur;
    private final ObjectMapper json;

    public IngestionVocaleService(BoutiqueService boutiques, Transcripteur transcripteur, ExtracteurLlm extracteur,
                                  ObjectMapper json) {
        this.boutiques = boutiques;
        this.transcripteur = transcripteur;
        this.extracteur = extracteur;
        this.json = json;
    }

    public ExtractionProduitDto analyser(UUID boutiqueId, MultipartFile audio) {
        Boutique boutique = boutiques.charger(boutiqueId);
        if (!boutique.aConsenti()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CONSENTEMENT_REQUIS",
                    "Acceptez les conditions d'utilisation du micro avant d'envoyer une note vocale.");
        }
        byte[] octets = lire(audio);
        if (octets.length == 0 || octets.length > TAILLE_MAX_AUDIO) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AUDIO_INVALIDE", "Audio vide ou supérieur à 10 Mo.");
        }
        TypeFichier type = TypeFichier.detecterAudio(octets).orElseThrow(() -> new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AUDIO_INVALIDE", "Formats acceptés : .wav et .m4a."));

        String transcription = transcripteur.transcrire(octets, "note." + type.extension(), type.mime());
        if (transcription == null || transcription.isBlank()) {
            return new ExtractionProduitDto("", null, null, null, false);
        }
        return valider(transcription, extracteur.extraireJson(transcription));
    }

    /** Validation stricte de la réponse du LLM : tout champ au mauvais type devient null. */
    ExtractionProduitDto valider(String transcription, String reponseLlm) {
        JsonNode racine;
        try {
            racine = json.readTree(reponseLlm == null ? "" : reponseLlm.trim());
        } catch (JsonProcessingException e) {
            racine = null;
        }
        if (racine == null || !racine.isObject()) {
            return new ExtractionProduitDto(transcription, null, null, null, false);
        }
        String nom = texte(racine.get("nom_produit"), 150);
        Integer prix = prix(racine.get("prix"));
        String taille = texte(racine.get("taille"), 20);
        return new ExtractionProduitDto(transcription, nom, prix, taille, nom != null && prix != null);
    }

    private static String texte(JsonNode noeud, int max) {
        if (noeud == null || noeud.isNull()) {
            return null;
        }
        if (noeud.isTextual() || noeud.isNumber()) {
            String t = Textes.nettoyer(noeud.asText(), max);
            return t == null || t.equalsIgnoreCase("null") ? null : t;
        }
        return null;
    }

    private static Integer prix(JsonNode noeud) {
        if (noeud == null || noeud.isNull()) {
            return null;
        }
        long valeur;
        if (noeud.isIntegralNumber()) {
            valeur = noeud.asLong();
        } else if (noeud.isNumber() && noeud.asDouble() == Math.rint(noeud.asDouble())) {
            valeur = (long) noeud.asDouble();
        } else if (noeud.isTextual() && noeud.asText().replaceAll("[\\s ]", "").matches("\\d{1,9}")) {
            valeur = Long.parseLong(noeud.asText().replaceAll("[\\s ]", ""));
        } else {
            return null;
        }
        return valeur > 0 && valeur <= 100_000_000 ? (int) valeur : null;
    }

    private static byte[] lire(MultipartFile fichier) {
        if (fichier == null) {
            throw ApiException.invalide("AUDIO_REQUIS", "Note vocale requise.");
        }
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
