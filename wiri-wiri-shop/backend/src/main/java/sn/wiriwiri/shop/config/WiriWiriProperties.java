package sn.wiriwiri.shop.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration applicative. Les secrets (JWT, AES, poivre OTP, clé IA) viennent
 * exclusivement de variables d'environnement : aucun secret n'est versionné.
 */
@Validated
@ConfigurationProperties(prefix = "wiriwiri")
public record WiriWiriProperties(
        @Valid @NotNull Securite securite,
        @Valid @NotNull Limitation limitation,
        @Valid @NotNull Ia ia,
        @Valid @NotNull Stockage stockage) {

    public record Securite(
            @NotBlank String jwtSecret,
            @NotNull Duration jwtDuree,
            @NotBlank String cleAes,
            @NotBlank String poivreOtp,
            @NotNull Duration otpDuree,
            @Min(1) int otpTentativesMax,
            List<String> originesAutorisees) {
    }

    public record Limitation(
            @Min(1) int requetesParMinute,
            @Min(1) int authRequetesParMinute) {
    }

    /**
     * Deux services indépendants, chacun compatible avec l'API OpenAI : OpenAI lui-même,
     * ou des serveurs locaux gratuits (Whisper pour la transcription, Ollama pour l'extraction).
     */
    public record Ia(
            @Valid @NotNull Service transcription,
            @Valid @NotNull Service extraction,
            String langue,
            @NotNull Duration delai) {
    }

    public record Service(
            @NotBlank String baseUrl,
            String cleApi,
            @NotBlank String modele) {

        public boolean aUneCle() {
            return cleApi != null && !cleApi.isBlank();
        }

        /** Une clé n'est exigée que pour l'API hébergée d'OpenAI ; un serveur local n'en a pas besoin. */
        public boolean estConfigure() {
            return aUneCle() || !baseUrl.contains("api.openai.com");
        }
    }

    public record Stockage(
            @NotBlank String repertoire,
            @NotBlank String urlPublique) {
    }
}
