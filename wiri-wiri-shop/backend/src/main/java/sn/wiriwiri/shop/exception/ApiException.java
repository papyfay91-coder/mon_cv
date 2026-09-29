package sn.wiriwiri.shop.exception;

import org.springframework.http.HttpStatus;

/** Erreur métier renvoyée au client sous forme de ProblemDetail (RFC 9457). */
public class ApiException extends RuntimeException {

    private final HttpStatus statut;
    private final String code;

    public ApiException(HttpStatus statut, String code, String message) {
        super(message);
        this.statut = statut;
        this.code = code;
    }

    public HttpStatus getStatut() {
        return statut;
    }

    public String getCode() {
        return code;
    }

    public static ApiException introuvable(String quoi) {
        return new ApiException(HttpStatus.NOT_FOUND, "INTROUVABLE", quoi + " introuvable.");
    }

    public static ApiException nonAuthentifie() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "NON_AUTHENTIFIE", "Authentification requise.");
    }

    public static ApiException invalide(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
