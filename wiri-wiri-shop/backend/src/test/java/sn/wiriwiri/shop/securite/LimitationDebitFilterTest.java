package sn.wiriwiri.shop.securite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LimitationDebitFilterTest {

    private final LimitationDebitFilter filtre = new LimitationDebitFilter(100, 3);

    private int appeler(String uri, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRemoteAddr(ip);
        MockHttpServletResponse rep = new MockHttpServletResponse();
        filtre.doFilter(req, rep, new MockFilterChain());
        return rep.getStatus();
    }

    @Test
    void bloqueLaForceBruteSurLesRoutesAuth() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(appeler("/api/auth/verification", "10.0.0.1")).isEqualTo(200);
        }
        assertThat(appeler("/api/auth/verification", "10.0.0.1")).isEqualTo(429);
        // Une autre IP n'est pas pénalisée ; les autres routes restent sous la limite globale
        assertThat(appeler("/api/auth/verification", "10.0.0.2")).isEqualTo(200);
        assertThat(appeler("/api/vitrine/boutiques/x", "10.0.0.1")).isEqualTo(200);
    }
}
