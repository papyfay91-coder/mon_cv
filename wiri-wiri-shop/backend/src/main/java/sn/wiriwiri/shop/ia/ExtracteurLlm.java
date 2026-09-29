package sn.wiriwiri.shop.ia;

/** Envoie la transcription au LLM et renvoie sa réponse brute (JSON attendu, non fiable). */
public interface ExtracteurLlm {

    /** Prompt système imposé par le cahier des charges (§6.1). */
    String PROMPT_SYSTEME = "Tu es un extracteur de données JSON pour le commerce à Dakar. "
            + "Analyse la phrase suivante en Wolof et extrait les informations sous forme de JSON strict "
            + "avec les clés suivantes : 'nom_produit' (traduit en français propre), "
            + "'prix' (un entier pur sans devise), 'taille' (si précisée, sinon null). "
            + "Ne renvoie rien d'autre que le JSON.";

    String extraireJson(String transcription);
}
