package sn.wiriwiri.shop.dto;

/**
 * Passerelle de paiement sans détention de fonds (BCEAO) : l'acheteur est redirigé
 * vers son propre compte Wave / Orange Money pour payer directement le vendeur.
 *
 * @param whatsapp    message de commande structuré, envoyé au vendeur
 * @param wave        lien de paiement Wave du vendeur (null s'il n'en a pas configuré)
 * @param orangeMoney composition du menu USSD Orange Money (#144#)
 * @param numeroVendeur numéro à créditer (Wave / Orange Money)
 */
public record LiensPaiement(String whatsapp, String wave, String orangeMoney, String numeroVendeur) {
}
