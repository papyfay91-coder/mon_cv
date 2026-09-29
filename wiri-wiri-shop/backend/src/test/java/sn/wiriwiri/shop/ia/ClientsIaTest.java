package sn.wiriwiri.shop.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import sn.wiriwiri.shop.config.WiriWiriProperties;

class ClientsIaTest {

    private final WiriWiriProperties proprietes = proprietes(
            new WiriWiriProperties.Service("https://api.test/v1", "cle", "whisper-1"),
            new WiriWiriProperties.Service("https://api.test/v1", "cle", "gpt-4o-mini"));

    private static WiriWiriProperties proprietes(WiriWiriProperties.Service transcription,
                                                 WiriWiriProperties.Service extraction) {
        return new WiriWiriProperties(null, null,
                new WiriWiriProperties.Ia(transcription, extraction, "wo", Duration.ofSeconds(5),
                        WiriWiriProperties.Hebergement.EXTERNE, "OpenAI"), null);
    }

    private final RestClient.Builder builder = RestClient.builder()
            .baseUrl("https://api.test/v1")
            .defaultHeader("Authorization", "Bearer cle");
    private final MockRestServiceServer serveur = MockRestServiceServer.bindTo(builder).build();
    private final RestClient client = builder.build();

    @Test
    void whisperEnvoieLeWolofPuisReplieEnDetectionAutoSiRefuse() {
        serveur.expect(requestTo("https://api.test/v1/audio/transcriptions"))
                .andExpect(header("Authorization", "Bearer cle"))
                .andExpect(content().string(containsString("name=\"language\"")))
                .andRespond(withBadRequest().body("{\"error\":{\"message\":\"Language 'wo' is not supported\"}}")
                        .contentType(MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("https://api.test/v1/audio/transcriptions"))
                .andExpect(content().string(not(containsString("name=\"language\""))))
                .andRespond(withSuccess("{\"text\":\" Robe bu bees \"}", MediaType.APPLICATION_JSON));

        String texte = new WhisperTranscripteur(client, proprietes).transcrire(new byte[]{1, 2}, "note.wav", "audio/wav");

        assertThat(texte).isEqualTo("Robe bu bees");
        serveur.verify();
    }

    @Test
    void llmUtiliseLePromptSystemeEtLeModeJson() {
        serveur.expect(requestTo("https://api.test/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value(ExtracteurLlm.PROMPT_SYSTEME))
                .andExpect(jsonPath("$.messages[1].content").value("Robe bu bees"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"prix\\\":1000}\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(new LlmExtracteur(client, proprietes).extraireJson("Robe bu bees")).isEqualTo("{\"prix\":1000}");
        serveur.verify();
    }

    @Test
    void serveurLocalSansCleFonctionne() {
        RestClient.Builder local = RestClient.builder().baseUrl("http://localhost:11434/v1");
        MockRestServiceServer ollama = MockRestServiceServer.bindTo(local).build();
        ollama.expect(requestTo("http://localhost:11434/v1/chat/completions"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(jsonPath("$.model").value("qwen2.5:7b"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}", MediaType.APPLICATION_JSON));

        var config = proprietes(
                new WiriWiriProperties.Service("http://localhost:9000/v1", "", "small"),
                new WiriWiriProperties.Service("http://localhost:11434/v1", "", "qwen2.5:7b"));
        assertThat(new LlmExtracteur(local.build(), config).extraireJson("Robe")).isEqualTo("{}");
        ollama.verify();
    }

    @Test
    void openAiSansCleEstIndisponible() {
        var config = proprietes(
                new WiriWiriProperties.Service("https://api.openai.com/v1", "", "whisper-1"),
                new WiriWiriProperties.Service("https://api.openai.com/v1", null, "gpt-4o-mini"));
        assertThatThrownBy(() -> new WhisperTranscripteur(client, config).transcrire(new byte[]{1}, "n.wav", "audio/wav"))
                .isInstanceOf(IaIndisponibleException.class);
        assertThatThrownBy(() -> new LlmExtracteur(client, config).extraireJson("x"))
                .isInstanceOf(IaIndisponibleException.class);
    }
}
