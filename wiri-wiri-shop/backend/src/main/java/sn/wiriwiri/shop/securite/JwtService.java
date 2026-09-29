package sn.wiriwiri.shop.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import sn.wiriwiri.shop.config.WiriWiriProperties;

/**
 * Jetons JWT éphémères signés en HMAC-SHA256. Le sujet est l'UUID de la boutique :
 * aucune donnée personnelle (téléphone, nom) n'est placée dans le jeton.
 */
@Service
public class JwtService {

    private static final String EMETTEUR = "wiri-wiri-shop";

    private final SecretKey cle;
    private final Duration duree;
    private final Clock horloge;

    public JwtService(WiriWiriProperties proprietes, Clock horloge) {
        byte[] octets = Decoders.BASE64.decode(proprietes.securite().jwtSecret());
        if (octets.length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 256 bits (32 octets en Base64)");
        }
        this.cle = Keys.hmacShaKeyFor(octets);
        this.duree = proprietes.securite().jwtDuree();
        this.horloge = horloge;
    }

    public String generer(UUID boutiqueId) {
        Instant maintenant = horloge.instant();
        return Jwts.builder()
                .issuer(EMETTEUR)
                .subject(boutiqueId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(duree)))
                .signWith(cle, Jwts.SIG.HS256)
                .compact();
    }

    public long dureeEnSecondes() {
        return duree.toSeconds();
    }

    /** Renvoie l'UUID de la boutique si le jeton est valide, signé par nous et non expiré. */
    public Optional<UUID> verifier(String jeton) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(cle)
                    .requireIssuer(EMETTEUR)
                    .clock(() -> Date.from(horloge.instant()))
                    .build()
                    .parseSignedClaims(jeton)
                    .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
