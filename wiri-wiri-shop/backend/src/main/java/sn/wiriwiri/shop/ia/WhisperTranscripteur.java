package sn.wiriwiri.shop.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import sn.wiriwiri.shop.config.WiriWiriProperties;

/**
 * Transcription via l'API OpenAI Whisper (/audio/transcriptions), langue "wo" (Wolof).
 * Si l'API rejette le code langue, on relance en détection automatique plutôt que d'échouer.
 */
@Component
public class WhisperTranscripteur implements Transcripteur {

    private static final Logger LOG = LoggerFactory.getLogger(WhisperTranscripteur.class);

    /** Amorce de vocabulaire : oriente la transcription vers le contexte commerce / FCFA à Dakar. */
    private static final String AMORCE = "Wolof, Dakar. Njëg bi, dërëm, junni, FCFA, taille, robe, sabador, basket.";

    private final RestClient client;
    private final WiriWiriProperties.Ia config;

    public WhisperTranscripteur(RestClient restClientIa, WiriWiriProperties proprietes) {
        this.client = restClientIa;
        this.config = proprietes.ia();
    }

    @Override
    public String transcrire(byte[] audio, String nomFichier, String typeMime) {
        if (config.cleApi() == null || config.cleApi().isBlank()) {
            throw new IaIndisponibleException("Service de transcription non configuré.");
        }
        String langue = config.langue();
        try {
            return appeler(audio, nomFichier, typeMime, langue);
        } catch (HttpClientErrorException.BadRequest e) {
            if (langue == null || langue.isBlank()) {
                throw new IaIndisponibleException("Transcription refusée par le service.");
            }
            LOG.warn("Whisper a refusé la langue '{}', nouvel essai en détection automatique", langue);
            return appeler(audio, nomFichier, typeMime, null);
        } catch (RestClientException e) {
            LOG.error("Échec de l'appel Whisper", e);
            throw new IaIndisponibleException("Service de transcription momentanément indisponible.");
        }
    }

    private String appeler(byte[] audio, String nomFichier, String typeMime, String langue) {
        HttpHeaders entetesFichier = new HttpHeaders();
        entetesFichier.setContentType(MediaType.parseMediaType(typeMime));
        ByteArrayResource ressource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return nomFichier;
            }
        };
        MultiValueMap<String, Object> corps = new LinkedMultiValueMap<>();
        corps.add("file", new HttpEntity<>(ressource, entetesFichier));
        corps.add("model", config.modeleTranscription());
        corps.add("response_format", "json");
        corps.add("temperature", "0");
        corps.add("prompt", AMORCE);
        if (langue != null && !langue.isBlank()) {
            corps.add("language", langue);
        }
        JsonNode reponse = client.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corps)
                .retrieve()
                .body(JsonNode.class);
        return reponse == null ? "" : reponse.path("text").asText("").trim();
    }
}
