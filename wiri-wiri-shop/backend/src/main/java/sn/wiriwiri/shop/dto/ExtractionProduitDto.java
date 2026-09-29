package sn.wiriwiri.shop.dto;

/**
 * Pré-remplissage de la fiche produit issu de la note vocale.
 * Les champs non reconnus sont null : le vendeur les complète avant validation finale.
 */
public record ExtractionProduitDto(
        String transcription,
        String nomProduit,
        Integer prix,
        String taille,
        boolean complet) {
}
