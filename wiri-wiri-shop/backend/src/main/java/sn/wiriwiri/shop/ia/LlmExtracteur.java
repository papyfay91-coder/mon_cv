package sn.wiriwiri.shop.ia;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import sn.wiriwiri.shop.config.WiriWiriProperties;

/** Extraction structurée via l'API Chat Completions, en mode de réponse JSON strict. */
@Component
public class LlmExtracteur implements ExtracteurLlm {

    private static final Logger LOG = LoggerFactory.getLogger(LlmExtracteur.class);

    private final RestClient client;
    private final WiriWiriProperties.Ia config;

    public LlmExtracteur(RestClient restClientIa, WiriWiriProperties proprietes) {
        this.client = restClientIa;
        this.config = proprietes.ia();
    }

    @Override
    public String extraireJson(String transcription) {
        if (config.cleApi() == null || config.cleApi().isBlank()) {
            throw new IaIndisponibleException("Service d'extraction non configuré.");
        }
        Map<String, Object> requete = Map.of(
                "model", config.modeleExtraction(),
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", PROMPT_SYSTEME),
                        // La transcription est une donnée non fiable : elle n'est jamais placée dans le prompt système.
                        Map.of("role", "user", "content", transcription)));
        try {
            JsonNode reponse = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requete)
                    .retrieve()
                    .body(JsonNode.class);
            return reponse == null ? "" : reponse.path("choices").path(0).path("message").path("content").asText("");
        } catch (RestClientException e) {
            LOG.error("Échec de l'appel au LLM d'extraction", e);
            throw new IaIndisponibleException("Service d'extraction momentanément indisponible.");
        }
    }
}
