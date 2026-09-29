package sn.wiriwiri.shop.ia;

/** Speech-to-Text. L'audio est traité en mémoire et n'est jamais persisté. */
public interface Transcripteur {

    String transcrire(byte[] audio, String nomFichier, String typeMime);
}
