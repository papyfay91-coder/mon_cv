package sn.wiriwiri.shop.securite;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import sn.wiriwiri.shop.config.WiriWiriProperties;

/**
 * Chiffrement au repos AES-256 en mode GCM (authentifié). Format stocké :
 * Base64( IV de 12 octets || texte chiffré || tag de 16 octets ).
 * Un IV aléatoire par valeur : deux numéros identiques donnent deux chiffrés différents.
 */
@Component
public class ChiffrementAes {

    private static final String ALGORITHME = "AES/GCM/NoPadding";
    private static final int TAILLE_IV = 12;
    private static final int TAILLE_TAG_BITS = 128;

    private final SecretKey cle;
    private final SecureRandom aleatoire = new SecureRandom();

    public ChiffrementAes(WiriWiriProperties proprietes) {
        byte[] octets = Base64.getDecoder().decode(proprietes.securite().cleAes());
        if (octets.length != 32) {
            throw new IllegalStateException("AES_KEY doit faire exactement 32 octets (AES-256), encodés en Base64");
        }
        this.cle = new SecretKeySpec(octets, "AES");
    }

    public String chiffrer(String clair) {
        if (clair == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAILLE_IV];
            aleatoire.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            byte[] chiffre = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + chiffre.length).put(iv).put(chiffre).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Échec du chiffrement", e);
        }
    }

    public String dechiffrer(String encode) {
        if (encode == null) {
            return null;
        }
        try {
            byte[] donnees = Base64.getDecoder().decode(encode);
            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, donnees, 0, TAILLE_IV));
            byte[] clair = cipher.doFinal(donnees, TAILLE_IV, donnees.length - TAILLE_IV);
            return new String(clair, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Échec du déchiffrement (clé incorrecte ou donnée altérée)", e);
        }
    }
}
