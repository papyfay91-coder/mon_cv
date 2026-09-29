package sn.wiriwiri.shop.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Détection du type réel d'un fichier par ses octets magiques : l'extension et le
 * Content-Type envoyés par le client ne sont jamais crus sur parole.
 */
public enum TypeFichier {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    WAV("wav", "audio/wav"),
    M4A("m4a", "audio/mp4");

    private final String extension;
    private final String mime;

    TypeFichier(String extension, String mime) {
        this.extension = extension;
        this.mime = mime;
    }

    public String extension() {
        return extension;
    }

    public String mime() {
        return mime;
    }

    public static Optional<TypeFichier> detecterImage(byte[] o) {
        if (commencePar(o, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return Optional.of(JPEG);
        }
        if (commencePar(o, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return Optional.of(PNG);
        }
        if (commencePar(o, 0, ascii("RIFF")) && commencePar(o, 8, ascii("WEBP"))) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<TypeFichier> detecterAudio(byte[] o) {
        if (commencePar(o, 0, ascii("RIFF")) && commencePar(o, 8, ascii("WAVE"))) {
            return Optional.of(WAV);
        }
        // Conteneur MPEG-4 (.m4a) : boîte "ftyp" à l'octet 4.
        if (commencePar(o, 4, ascii("ftyp"))) {
            return Optional.of(M4A);
        }
        return Optional.empty();
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static boolean commencePar(byte[] octets, int decalage, byte[] signature) {
        if (octets == null || octets.length < decalage + signature.length) {
            return false;
        }
        return Arrays.equals(octets, decalage, decalage + signature.length, signature, 0, signature.length);
    }
}
