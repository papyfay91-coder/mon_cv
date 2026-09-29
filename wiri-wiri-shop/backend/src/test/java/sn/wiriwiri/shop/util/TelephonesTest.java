package sn.wiriwiri.shop.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sn.wiriwiri.shop.exception.ApiException;

class TelephonesTest {

    @Test
    void normaliseLesFormatsCourants() {
        assertThat(Telephones.normaliser("77 123 45 67")).isEqualTo("+221771234567");
        assertThat(Telephones.normaliser("00221 76-123-45-67")).isEqualTo("+221761234567");
        assertThat(Telephones.normaliser("221781234567")).isEqualTo("+221781234567");
        assertThat(Telephones.normaliser("+221 33 823 45 67")).isEqualTo("+221338234567");
    }

    @Test
    void refuseLesNumerosEtrangersOuMalformes() {
        assertThatThrownBy(() -> Telephones.normaliser("+33612345678")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Telephones.normaliser("123")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Telephones.normaliser("' OR 1=1 --")).isInstanceOf(ApiException.class);
    }

    @Test
    void masquePourLesJournaux() {
        assertThat(Telephones.masquer("+221771234567")).isEqualTo("+221*******67");
    }
}
