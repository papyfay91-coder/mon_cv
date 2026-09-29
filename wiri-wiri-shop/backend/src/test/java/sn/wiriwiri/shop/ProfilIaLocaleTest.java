package sn.wiriwiri.shop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import sn.wiriwiri.shop.config.WiriWiriProperties;
import sn.wiriwiri.shop.dto.BoutiqueDto;

/** Le profil « ia-locale » pointe vers les serveurs gratuits de la machine, sans clé. */
@SpringBootTest
@ActiveProfiles({"test", "ia-locale"})
@Import(SmsCapture.class)
class ProfilIaLocaleTest {

    @Autowired
    WiriWiriProperties proprietes;

    @Test
    void servicesLocauxSansCle() {
        var ia = proprietes.ia();
        assertThat(ia.transcription().baseUrl()).isEqualTo("http://localhost:9000/v1");
        assertThat(ia.extraction().baseUrl()).isEqualTo("http://localhost:11434/v1");
        assertThat(ia.extraction().modele()).isEqualTo("qwen2.5:7b");
        assertThat(ia.transcription().aUneCle()).isFalse();
        assertThat(ia.transcription().estConfigure()).isTrue();
        assertThat(ia.extraction().estConfigure()).isTrue();
        // Affiché au vendeur : rien n'est envoyé à un service extérieur
        assertThat(ia.hebergement()).isEqualTo(WiriWiriProperties.Hebergement.LOCAL);
        assertThat(BoutiqueDto.TraitementVocal.de(ia).fournisseur()).isNull();
    }
}
