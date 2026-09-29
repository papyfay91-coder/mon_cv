package sn.wiriwiri.shop.securite;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limitation du débit par adresse IP (Bucket4j) :
 * <ul>
 *   <li>un seau global pour toutes les routes (anti-DDoS applicatif) ;</li>
 *   <li>un seau beaucoup plus strict pour /api/auth/** (anti brute-force des codes OTP).</li>
 * </ul>
 * Les seaux vivent dans un cache borné qui expire, pour ne pas saturer la mémoire.
 * Derrière un reverse proxy, activer server.forward-headers-strategy pour obtenir la vraie IP.
 */
public class LimitationDebitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> seauxGlobaux;
    private final Cache<String, Bucket> seauxAuth;
    private final int limiteGlobale;
    private final int limiteAuth;

    public LimitationDebitFilter(int limiteGlobaleParMinute, int limiteAuthParMinute) {
        this.limiteGlobale = limiteGlobaleParMinute;
        this.limiteAuth = limiteAuthParMinute;
        this.seauxGlobaux = nouveauCache();
        this.seauxAuth = nouveauCache();
    }

    private static Cache<String, Bucket> nouveauCache() {
        return Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
    }

    private static Bucket seau(int capaciteParMinute) {
        return Bucket.builder()
                .addLimit(limite -> limite.capacity(capaciteParMinute)
                        .refillGreedy(capaciteParMinute, Duration.ofMinutes(1)))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String ip = requete.getRemoteAddr();

        if (requete.getRequestURI().startsWith("/api/auth/")) {
            Bucket seauAuth = seauxAuth.get(ip, cle -> seau(limiteAuth));
            if (!consommer(seauAuth, reponse)) {
                return;
            }
        }
        Bucket seauGlobal = seauxGlobaux.get(ip, cle -> seau(limiteGlobale));
        if (!consommer(seauGlobal, reponse)) {
            return;
        }
        chaine.doFilter(requete, reponse);
    }

    private boolean consommer(Bucket seau, HttpServletResponse reponse) throws IOException {
        ConsumptionProbe sonde = seau.tryConsumeAndReturnRemaining(1);
        if (sonde.isConsumed()) {
            reponse.setHeader("X-RateLimit-Remaining", Long.toString(sonde.getRemainingTokens()));
            return true;
        }
        long attente = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(sonde.getNanosToWaitForRefill()));
        reponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        reponse.setHeader("Retry-After", Long.toString(attente));
        reponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        reponse.getWriter().write(
                "{\"status\":429,\"code\":\"TROP_DE_REQUETES\",\"detail\":\"Trop de requêtes, réessayez plus tard.\"}");
        return false;
    }
}
