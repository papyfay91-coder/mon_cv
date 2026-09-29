package sn.wiriwiri.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandeServiceTest {

    @Test
    void formateLesMontantsEnFcfa() {
        assertThat(CommandeService.formaterMontant(1500)).isEqualTo("1 500");
        assertThat(CommandeService.formaterMontant(250000)).isEqualTo("250 000");
    }
}
