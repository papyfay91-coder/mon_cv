package sn.wiriwiri.shop.securite;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import sn.wiriwiri.shop.exception.ApiException;

/** Accès à l'identité du vendeur authentifié (UUID de sa boutique). */
public final class Vendeur {

    private Vendeur() {
    }

    public static UUID boutiqueCourante() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID id)) {
            throw ApiException.nonAuthentifie();
        }
        return id;
    }
}
