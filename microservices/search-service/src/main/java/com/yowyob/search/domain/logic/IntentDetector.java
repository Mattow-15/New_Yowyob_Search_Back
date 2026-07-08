package com.yowyob.search.domain.logic;

import java.util.List;
import java.util.Map;

public class IntentDetector {

    public enum Intent {
        PRODUCT_SEARCH,
        PROFILE_SEARCH,
        RECOMMENDATION,
        INFORMATION,
        NAVIGATION,
        GENERAL
    }

    // Ordre de priorité : du plus spécifique au plus général.
    // Map.of est non-ordonné → on utilise une List pour garantir l'ordre d'évaluation.
    private static final List<Map.Entry<Intent, List<String>>> INTENT_RULES = List.of(
        Map.entry(Intent.PRODUCT_SEARCH, List.of(
            "acheter", "commander", "disponible en stock", "prix d'achat",
            "ajouter au panier", "passer commande", "livraison", "en vente"
        )),
        Map.entry(Intent.PROFILE_SEARCH, List.of(
            "profil", "fiche", "entreprise", "présentation de", "coordonnées de"
        )),
        Map.entry(Intent.NAVIGATION, List.of(
            "comment aller", "itinéraire", "chemin", "route",
            "direction", "trajet", "aller à", "se rendre"
        )),
        Map.entry(Intent.INFORMATION, List.of(
            "horaire", "heure", "ouvert", "fermé", "prix",
            "tarif", "numéro", "téléphone", "adresse", "contact"
        )),
        Map.entry(Intent.RECOMMENDATION, List.of(
            "meilleur", "mieux", "recommande", "conseil",
            "top", "où manger", "où trouver",
            "suggestion", "propose", "bonne adresse",
            "manger", "faim", "boire", "soif",
            "veux trouver", "cherche", "trouver",
            "besoin", "envie", "chercher"
        ))
    );

    public Intent detect(String query) {
        if (query == null || query.isBlank()) return Intent.GENERAL;
        String lower = query.toLowerCase();

        for (var entry : INTENT_RULES) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return Intent.GENERAL;
    }
}
