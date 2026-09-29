package sn.wiriwiri.shop.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Les erreurs internes ne divulguent jamais de trace ni de détail technique au client. */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger LOG = LoggerFactory.getLogger(GestionnaireErreurs.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail metier(ApiException e) {
        return probleme(e.getStatut(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ProblemDetail validation(BindException e) {
        Map<String, String> champs = new LinkedHashMap<>();
        e.getFieldErrors().forEach(f -> champs.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail pd = probleme(HttpStatus.BAD_REQUEST, "VALIDATION", "Données invalides.");
        pd.setProperty("champs", champs);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail contrainte(ConstraintViolationException e) {
        return probleme(HttpStatus.BAD_REQUEST, "VALIDATION", "Données invalides.");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail requeteIllisible(Exception e) {
        return probleme(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE", "Requête invalide.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail tropGros(MaxUploadSizeExceededException e) {
        return probleme(HttpStatus.PAYLOAD_TOO_LARGE, "FICHIER_TROP_VOLUMINEUX", "Fichier trop volumineux.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail typeNonSupporte(HttpMediaTypeNotSupportedException e) {
        return probleme(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TYPE_NON_SUPPORTE", "Type de contenu non supporté.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail methode(HttpRequestMethodNotSupportedException e) {
        return probleme(HttpStatus.METHOD_NOT_ALLOWED, "METHODE_NON_AUTORISEE", "Méthode non autorisée.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail ressource(NoResourceFoundException e) {
        return probleme(HttpStatus.NOT_FOUND, "INTROUVABLE", "Ressource introuvable.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail interne(Exception e) {
        LOG.error("Erreur interne", e);
        return probleme(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Une erreur interne est survenue.");
    }

    private static ProblemDetail probleme(HttpStatus statut, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, detail);
        pd.setProperty("code", code);
        return pd;
    }
}
