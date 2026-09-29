package sn.wiriwiri.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TypeFichierTest {

    @Test
    void detecteLesAudiosParOctetsMagiques() {
        byte[] wav = "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1);
        byte[] m4a = "\0\0\0 ftypM4A ".getBytes(StandardCharsets.ISO_8859_1);
        assertThat(TypeFichier.detecterAudio(wav)).contains(TypeFichier.WAV);
        assertThat(TypeFichier.detecterAudio(m4a)).contains(TypeFichier.M4A);
        assertThat(TypeFichier.detecterAudio("<?php echo 1;".getBytes())).isEmpty();
        assertThat(TypeFichier.detecterAudio(new byte[2])).isEmpty();
    }

    @Test
    void detecteLesImages() {
        assertThat(TypeFichier.detecterImage(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).contains(TypeFichier.JPEG);
        assertThat(TypeFichier.detecterImage("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1)))
                .contains(TypeFichier.WEBP);
        assertThat(TypeFichier.detecterImage("<svg onload=alert(1)>".getBytes())).isEmpty();
    }
}
