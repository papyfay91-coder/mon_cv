package sn.wiriwiri.shop.config;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import sn.wiriwiri.shop.securite.JwtAuthenticationFilter;
import sn.wiriwiri.shop.securite.JwtService;
import sn.wiriwiri.shop.securite.LimitationDebitFilter;

/**
 * Chaîne de filtres Spring Security 6 : tout est verrouillé par défaut (deny by default).
 * Seuls sont publics : la connexion OTP, la lecture (GET) des vitrines et des images,
 * le passage de commande par un client, et la sonde de santé.
 */
@Configuration
public class SecurityConfig {

    private static final String JSON_401 =
            "{\"status\":401,\"code\":\"NON_AUTHENTIFIE\",\"detail\":\"Authentification requise.\"}";
    private static final String JSON_403 =
            "{\"status\":403,\"code\":\"ACCES_REFUSE\",\"detail\":\"Accès refusé.\"}";

    @Bean
    public SecurityFilterChain chaineDeFiltres(HttpSecurity http, JwtService jwtService,
                                               WiriWiriProperties proprietes) throws Exception {
        var limitation = proprietes.limitation();
        http
                // API stateless sans cookie de session : le CSRF n'a pas de surface d'attaque.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/otp", "/api/auth/verification").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vitrine/**", "/fichiers/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/vitrine/commandes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/vendeur/**").hasRole("VENDEUR")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, rep, ex) -> ecrire(rep, HttpServletResponse.SC_UNAUTHORIZED, JSON_401))
                        .accessDeniedHandler((req, rep, ex) -> ecrire(rep, HttpServletResponse.SC_FORBIDDEN, JSON_403)))
                .addFilterBefore(new LimitationDebitFilter(limitation.requetesParMinute(), limitation.authRequetesParMinute()),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void ecrire(HttpServletResponse reponse, int statut, String corps) throws java.io.IOException {
        reponse.setStatus(statut);
        reponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        reponse.getWriter().write(corps);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(WiriWiriProperties proprietes) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origines = proprietes.securite().originesAutorisees();
        config.setAllowedOrigins(origines == null ? List.of() : origines);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Aucun utilisateur par mot de passe : empêche Spring Boot de générer un compte par défaut. */
    @Bean
    public UserDetailsService aucunUtilisateur() {
        return new InMemoryUserDetailsManager();
    }
}
