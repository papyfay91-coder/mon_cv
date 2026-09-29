package sn.wiriwiri.shop.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.wiriwiri.shop.config.WiriWiriProperties;
import sn.wiriwiri.shop.dto.JetonReponse;
import sn.wiriwiri.shop.dto.VerificationOtp;
import sn.wiriwiri.shop.entity.Boutique;
import sn.wiriwiri.shop.entity.CodeOtp;
import sn.wiriwiri.shop.exception.ApiException;
import sn.wiriwiri.shop.repository.BoutiqueRepository;
import sn.wiriwiri.shop.repository.CodeOtpRepository;
import sn.wiriwiri.shop.securite.JwtService;
import sn.wiriwiri.shop.sms.EnvoiSms;
import sn.wiriwiri.shop.util.Telephones;
import sn.wiriwiri.shop.util.Textes;

/**
 * Connexion sans mot de passe par code OTP envoyé par SMS.
 * <ul>
 *   <li>code à 6 chiffres tiré par SecureRandom, valable quelques minutes ;</li>
 *   <li>stockage d'un condensat HMAC-SHA256 poivré, jamais du code ;</li>
 *   <li>comparaison en temps constant, nombre de tentatives borné ;</li>
 *   <li>délai minimal entre deux envois pour un même numéro (anti-spam SMS).</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Duration DELAI_RENVOI = Duration.ofSeconds(60);

    private final CodeOtpRepository codes;
    private final BoutiqueRepository boutiques;
    private final EnvoiSms sms;
    private final JwtService jwt;
    private final Clock horloge;
    private final SecretKeySpec poivre;
    private final Duration dureeOtp;
    private final int tentativesMax;
    private final SecureRandom aleatoire = new SecureRandom();

    public AuthService(CodeOtpRepository codes, BoutiqueRepository boutiques, EnvoiSms sms, JwtService jwt,
                       Clock horloge, WiriWiriProperties proprietes) {
        this.codes = codes;
        this.boutiques = boutiques;
        this.sms = sms;
        this.jwt = jwt;
        this.horloge = horloge;
        this.poivre = new SecretKeySpec(Base64.getDecoder().decode(proprietes.securite().poivreOtp()), "HmacSHA256");
        this.dureeOtp = proprietes.securite().otpDuree();
        this.tentativesMax = proprietes.securite().otpTentativesMax();
    }

    /** Toujours silencieux sur l'existence d'un compte (pas d'énumération des numéros). */
    @Transactional
    public void demanderCode(String telephoneBrut) {
        String telephone = Telephones.normaliser(telephoneBrut);
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Optional<CodeOtp> existant = codes.verrouiller(telephone);
        if (existant.isPresent()) {
            LocalDateTime emission = existant.get().getDateExpiration().minus(dureeOtp);
            if (maintenant.isBefore(emission.plus(DELAI_RENVOI))) {
                return;
            }
        }
        String code = String.format("%06d", aleatoire.nextInt(1_000_000));
        String hash = hacher(telephone, code);
        LocalDateTime expiration = maintenant.plus(dureeOtp);
        existant.ifPresentOrElse(
                c -> c.remplacer(hash, expiration),
                () -> codes.save(new CodeOtp(telephone, hash, expiration)));
        sms.envoyer(telephone, "Wiri-Wiri Shop : votre code est " + code
                + ". Valable " + dureeOtp.toMinutes() + " min. Ne le partagez avec personne.");
    }

    /** Les échecs sont comptabilisés même si une exception est levée (pas de rollback). */
    @Transactional(noRollbackFor = ApiException.class)
    public JetonReponse verifierCode(VerificationOtp demande) {
        String telephone = Telephones.normaliser(demande.telephone());
        CodeOtp otp = codes.verrouiller(telephone).orElseThrow(AuthService::codeInvalide);

        if (LocalDateTime.now(horloge).isAfter(otp.getDateExpiration()) || otp.getTentatives() >= tentativesMax) {
            codes.delete(otp);
            throw codeInvalide();
        }
        byte[] attendu = otp.getCodeHash().getBytes(StandardCharsets.US_ASCII);
        byte[] fourni = hacher(telephone, demande.code()).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(attendu, fourni)) {
            otp.incrementerTentatives();
            if (otp.getTentatives() >= tentativesMax) {
                codes.delete(otp);
            }
            throw codeInvalide();
        }

        Optional<Boutique> existante = boutiques.findByTelephone(telephone);
        boolean nouveauCompte = existante.isEmpty();
        Boutique boutique;
        if (nouveauCompte) {
            String nom = Textes.nettoyer(demande.nomVendeur(), 100);
            if (nom == null || nom.length() < 2) {
                // Le code reste valable : l'application demande le nom puis renvoie le même code.
                throw ApiException.invalide("NOM_REQUIS", "Indiquez le nom de votre boutique pour créer votre compte.");
            }
            boutique = boutiques.save(new Boutique(nom, telephone, LocalDateTime.now(horloge)));
        } else {
            boutique = existante.get();
            if (!boutique.isActif()) {
                codes.delete(otp);
                throw new ApiException(HttpStatus.FORBIDDEN, "COMPTE_SUSPENDU", "Ce compte est suspendu.");
            }
        }
        codes.delete(otp);
        return new JetonReponse(jwt.generer(boutique.getId()), "Bearer", jwt.dureeEnSecondes(), nouveauCompte);
    }

    @Scheduled(fixedDelayString = "PT15M")
    @Transactional
    public void purgerCodesExpires() {
        codes.purgerExpires(LocalDateTime.now(horloge));
    }

    private static ApiException codeInvalide() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "CODE_INVALIDE", "Code invalide ou expiré.");
    }

    private String hacher(String telephone, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(poivre);
            return HexFormat.of().formatHex(mac.doFinal((telephone + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
