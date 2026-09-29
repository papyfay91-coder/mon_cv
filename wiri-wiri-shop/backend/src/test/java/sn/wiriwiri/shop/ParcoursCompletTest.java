package sn.wiriwiri.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sn.wiriwiri.shop.ia.ExtracteurLlm;
import sn.wiriwiri.shop.ia.Transcripteur;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SmsCapture.class)
class ParcoursCompletTest {

    static final byte[] WAV = concat("RIFF".getBytes(StandardCharsets.US_ASCII), new byte[]{0, 0, 0, 0},
            "WAVEfmt ".getBytes(StandardCharsets.US_ASCII), new byte[64]);
    static final byte[] PNG = concat(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, new byte[64]);

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;

    @MockBean
    Transcripteur transcripteur;
    @MockBean
    ExtracteurLlm extracteur;

    @Test
    void parcoursVendeurEtClient() throws Exception {
        String jeton = connecter("77 123 45 67", "Awa Fashion");

        // Routes vendeur verrouillées sans jeton
        mvc.perform(get("/api/vendeur/boutique")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/vendeur/boutique").header(HttpHeaders.AUTHORIZATION, "Bearer faux.jeton.ici"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/vendeur/boutique").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomVendeur").value("Awa Fashion"))
                .andExpect(jsonPath("$.telephone").value("+221771234567"))
                .andExpect(jsonPath("$.consentementDonne").value(false));

        // Pas de micro sans consentement explicite (CDP)
        MockMultipartFile audio = new MockMultipartFile("audio", "note.wav", "audio/wav", WAV);
        mvc.perform(multipart("/api/vendeur/produits/analyse-vocale").file(audio)
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CONSENTEMENT_REQUIS"));

        mvc.perform(post("/api/vendeur/consentement").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentementDonne").value(true));

        // Voice-to-Store
        when(transcripteur.transcrire(any(), anyString(), anyString()))
                .thenReturn("Robe bu bees, ñaari fukki junni, taille M");
        when(extracteur.extraireJson(anyString()))
                .thenReturn("{\"nom_produit\":\"Robe neuve <script>\",\"prix\":20000,\"taille\":\"M\"}");
        mvc.perform(multipart("/api/vendeur/produits/analyse-vocale").file(audio)
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomProduit").value("Robe neuve"))
                .andExpect(jsonPath("$.prix").value(20000))
                .andExpect(jsonPath("$.taille").value("M"))
                .andExpect(jsonPath("$.complet").value(true));

        // Un faux audio (octets magiques absents) est refusé
        mvc.perform(multipart("/api/vendeur/produits/analyse-vocale")
                        .file(new MockMultipartFile("audio", "x.wav", "audio/wav", "pas un audio".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isUnsupportedMediaType());

        // Publication après validation par le vendeur
        String produitJson = mvc.perform(multipart("/api/vendeur/produits")
                        .file(new MockMultipartFile("image", "robe.png", "image/png", PNG))
                        .param("nomProduit", "Robe neuve")
                        .param("prix", "20000")
                        .param("taille", "M")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.endsWith(".png")))
                .andReturn().getResponse().getContentAsString();
        UUID produitId = UUID.fromString(json.readTree(produitJson).get("id").asText());

        // Prix négatif refusé
        mvc.perform(multipart("/api/vendeur/produits")
                        .file(new MockMultipartFile("image", "robe.png", "image/png", PNG))
                        .param("nomProduit", "Robe").param("prix", "-5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isBadRequest());

        // Vitrine publique en GET
        JsonNode profil = json.readTree(mvc.perform(get("/api/vendeur/boutique")
                .header(HttpHeaders.AUTHORIZATION, bearer(jeton))).andReturn().getResponse().getContentAsString());
        String boutiqueId = profil.get("id").asText();
        mvc.perform(get("/api/vitrine/boutiques/" + boutiqueId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("max-age=60")))
                .andExpect(jsonPath("$.nomVendeur").value("Awa Fashion"))
                .andExpect(jsonPath("$.produits[0].nomProduit").value("Robe neuve"))
                .andExpect(jsonPath("$.produits[0].prix").value(20000));

        // Le vendeur configure son lien Wave
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/vendeur/boutique")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lienWave\":\"https://pay.wave.com/m/M_awa123/c/sn/\"}"))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/vendeur/boutique")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jeton))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lienWave\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        // Commande client : liens de paiement direct, aucun encaissement
        mvc.perform(post("/api/vitrine/commandes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produitId\":\"" + produitId + "\",\"telephoneClient\":\"+221 76 000 11 22\","
                                + "\"quartierLivraison\":\"Parcelles Assainies\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(20000))
                .andExpect(jsonPath("$.paiement.whatsapp").value(org.hamcrest.Matchers.startsWith("https://wa.me/221771234567?text=")))
                .andExpect(jsonPath("$.paiement.wave").value("https://pay.wave.com/m/M_awa123/c/sn/?amount=20000"))
                .andExpect(jsonPath("$.paiement.orangeMoney").value("tel:%23144%23"));

        // Le numéro du client est chiffré au repos (AES-256-GCM)
        String colonne = jdbc.queryForObject("select telephone_client from commandes", String.class);
        assertThat(colonne).doesNotContain("760001122").hasSizeGreaterThan(40);

        mvc.perform(get("/api/vendeur/commandes").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].telephoneClient").value("+221760001122"))
                .andExpect(jsonPath("$[0].quartierLivraison").value("Parcelles Assainies"));

        // Anti-IDOR : un autre vendeur ne peut pas modifier ce produit
        String autre = connecter("781112233", "Modou Sneakers");
        mvc.perform(patch("/api/vendeur/produits/" + produitId).header(HttpHeaders.AUTHORIZATION, bearer(autre))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prix\":1}"))
                .andExpect(status().isNotFound());

        // Droit à l'oubli : purge complète
        mvc.perform(delete("/api/vendeur/compte").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/vitrine/boutiques/" + boutiqueId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/vendeur/boutique").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from produits where boutique_id = ?", Integer.class,
                UUID.fromString(boutiqueId))).isZero();
        assertThat(jdbc.queryForObject("select count(*) from commandes", Integer.class)).isZero();
    }

    @Test
    void codeOtpProtegeContreLaForceBrute() throws Exception {
        String tel = "+221701234599";
        mvc.perform(post("/api/auth/otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telephone\":\"" + tel + "\"}")).andExpect(status().isAccepted());
        String bonCode = SmsCapture.DERNIER_CODE.get(tel);
        String mauvais = bonCode.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/verification").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"telephone\":\"" + tel + "\",\"code\":\"" + mauvais + "\",\"nomVendeur\":\"Test\"}"))
                    .andExpect(status().isUnauthorized());
        }
        // Le bon code est désormais invalidé
        mvc.perform(post("/api/auth/verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + tel + "\",\"code\":\"" + bonCode + "\",\"nomVendeur\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routesInconnuesEtEcrituresPubliquesRefusees() throws Exception {
        mvc.perform(get("/api/admin")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/vitrine/boutiques/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telephone\":\"0612345678\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private String connecter(String telephone, String nom) throws Exception {
        mvc.perform(post("/api/auth/otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telephone\":\"" + telephone + "\"}")).andExpect(status().isAccepted());
        String normalise = sn.wiriwiri.shop.util.Telephones.normaliser(telephone);
        String code = SmsCapture.DERNIER_CODE.get(normalise);
        assertThat(code).matches("\\d{6}");

        // Première connexion sans nom : le code reste valable
        mvc.perform(post("/api/auth/verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOM_REQUIS"));

        String reponse = mvc.perform(post("/api/auth/verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"code\":\"" + code + "\",\"nomVendeur\":\"" + nom + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauCompte").value(true))
                .andReturn().getResponse().getContentAsString();

        // Usage unique
        mvc.perform(post("/api/auth/verification").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isUnauthorized());
        return json.readTree(reponse).get("jeton").asText();
    }

    private static String bearer(String jeton) {
        return "Bearer " + jeton;
    }

    private static byte[] concat(byte[]... parties) {
        int n = 0;
        for (byte[] p : parties) {
            n += p.length;
        }
        byte[] r = new byte[n];
        int i = 0;
        for (byte[] p : parties) {
            System.arraycopy(p, 0, r, i, p.length);
            i += p.length;
        }
        return r;
    }
}
