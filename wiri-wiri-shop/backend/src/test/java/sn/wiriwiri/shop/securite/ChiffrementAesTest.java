package sn.wiriwiri.shop.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import sn.wiriwiri.shop.config.WiriWiriProperties;

class ChiffrementAesTest {

    static WiriWiriProperties proprietes(String cleAes) {
        return new WiriWiriProperties(
                new WiriWiriProperties.Securite("x", Duration.ofMinutes(30), cleAes, "x", Duration.ofMinutes(5), 5, List.of()),
                null, null, null);
    }

    private final ChiffrementAes aes = new ChiffrementAes(
            proprietes(Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes())));

    @Test
    void allerRetour() {
        String c = aes.chiffrer("+221771234567");
        assertThat(c).doesNotContain("771234567");
        assertThat(aes.dechiffrer(c)).isEqualTo("+221771234567");
    }

    @Test
    void ivAleatoire() {
        assertThat(aes.chiffrer("+221771234567")).isNotEqualTo(aes.chiffrer("+221771234567"));
    }

    @Test
    void alterationDetectee() {
        byte[] octets = Base64.getDecoder().decode(aes.chiffrer("+221771234567"));
        octets[octets.length - 1] ^= 1;
        assertThatThrownBy(() -> aes.dechiffrer(Base64.getEncoder().encodeToString(octets)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cleDeTailleIncorrecteRefusee() {
        assertThatThrownBy(() -> new ChiffrementAes(proprietes(Base64.getEncoder().encodeToString(new byte[16]))))
                .isInstanceOf(IllegalStateException.class);
    }
}
