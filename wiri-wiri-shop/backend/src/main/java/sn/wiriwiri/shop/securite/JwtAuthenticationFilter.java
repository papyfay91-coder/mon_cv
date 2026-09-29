package sn.wiriwiri.shop.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Extrait le jeton "Bearer" et authentifie le vendeur, sans aucune session serveur. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String entete = requete.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete != null && entete.startsWith(PREFIXE)) {
            jwtService.verifier(entete.substring(PREFIXE.length()).trim()).ifPresent(boutiqueId -> {
                var authentification = new UsernamePasswordAuthenticationToken(
                        boutiqueId, null, List.of(new SimpleGrantedAuthority("ROLE_VENDEUR")));
                SecurityContextHolder.getContext().setAuthentication(authentification);
            });
        }
        chaine.doFilter(requete, reponse);
    }
}
