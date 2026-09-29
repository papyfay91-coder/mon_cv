package sn.wiriwiri.shop.dto;

public record JetonReponse(String jeton, String type, long expireDansSecondes, boolean nouveauCompte) {
}
