package sn.wiriwiri.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sn.wiriwiri.shop.dto.ExtractionProduitDto;

class ValidationExtractionTest {

    private final IngestionVocaleService service = new IngestionVocaleService(null, null, null, new ObjectMapper());

    @Test
    void reponseConforme() {
        ExtractionProduitDto r = service.valider("t", "{\"nom_produit\":\"Basket Nike\",\"prix\":35000,\"taille\":\"42\"}");
        assertThat(r.nomProduit()).isEqualTo("Basket Nike");
        assertThat(r.prix()).isEqualTo(35000);
        assertThat(r.taille()).isEqualTo("42");
        assertThat(r.complet()).isTrue();
    }

    @Test
    void prixEnTexteOuDecimalEntierAccepte() {
        assertThat(service.valider("t", "{\"nom_produit\":\"a\",\"prix\":\"15 000\"}").prix()).isEqualTo(15000);
        assertThat(service.valider("t", "{\"nom_produit\":\"a\",\"prix\":15000.0}").prix()).isEqualTo(15000);
    }

    @Test
    void typesInvalidesDeviennentNull() {
        ExtractionProduitDto r = service.valider("t",
                "{\"nom_produit\":{\"x\":1},\"prix\":\"quinze mille\",\"taille\":[\"M\"]}");
        assertThat(r.nomProduit()).isNull();
        assertThat(r.prix()).isNull();
        assertThat(r.taille()).isNull();
        assertThat(r.complet()).isFalse();
    }

    @Test
    void prixNegatifDecimalOuDemesureRefuse() {
        assertThat(service.valider("t", "{\"prix\":-100}").prix()).isNull();
        assertThat(service.valider("t", "{\"prix\":0}").prix()).isNull();
        assertThat(service.valider("t", "{\"prix\":99.5}").prix()).isNull();
        assertThat(service.valider("t", "{\"prix\":99999999999}").prix()).isNull();
    }

    @Test
    void reponseNonJsonOuTexteLibre() {
        assertThat(service.valider("t", "Voici le JSON : {...}").complet()).isFalse();
        assertThat(service.valider("t", null).complet()).isFalse();
        assertThat(service.valider("t", "[1,2]").complet()).isFalse();
    }

    @Test
    void tailleNullTextuelleIgnoreeEtHtmlNettoye() {
        ExtractionProduitDto r = service.valider("t",
                "{\"nom_produit\":\"<img src=x onerror=alert(1)>Sac\",\"prix\":5000,\"taille\":\"null\"}");
        assertThat(r.nomProduit()).isEqualTo("Sac");
        assertThat(r.taille()).isNull();
    }
}
