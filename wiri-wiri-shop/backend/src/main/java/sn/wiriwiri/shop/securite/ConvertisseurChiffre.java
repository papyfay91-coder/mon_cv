package sn.wiriwiri.shop.securite;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/** Convertisseur JPA : chiffre la colonne à l'écriture, la déchiffre à la lecture. */
@Component
@Converter
public class ConvertisseurChiffre implements AttributeConverter<String, String> {

    private final ChiffrementAes chiffrement;

    public ConvertisseurChiffre(ChiffrementAes chiffrement) {
        this.chiffrement = chiffrement;
    }

    @Override
    public String convertToDatabaseColumn(String valeur) {
        return chiffrement.chiffrer(valeur);
    }

    @Override
    public String convertToEntityAttribute(String colonne) {
        return chiffrement.dechiffrer(colonne);
    }
}
