package com.yowyob.search.domain.logic;

import java.util.List;
import java.util.Map;

public class IntentDetector {

    public enum Intent {
        RECOMMENDATION,
        INFORMATION,
        NAVIGATION,
        GENERAL
    }

    private static final Map<Intent, List<String>> INTENT_KEYWORDS =
        Map.of(
            Intent.RECOMMENDATION, List.of(
                "meilleur", "mieux", "recommande", "conseil",
                "top", "où manger", "où acheter", "où trouver",
                "suggestion", "propose", "bonne adresse",
                "manger", "faim", "boire", "soif",
                "veux", "veux trouver", "cherche", "trouver",
                "besoin", "envie", "chercher", "acheter"
            ),
            Intent.INFORMATION, List.of(
                "horaire", "heure", "ouvert", "fermé", "prix",
                "tarif", "numéro", "téléphone", "adresse", "contact"
            ),
            Intent.NAVIGATION, List.of(
                "comment aller", "itinéraire", "chemin", "route",
                "direction", "trajet", "aller à", "se rendre"
            )
        );

    public Intent detect(String query) {
        if (query == null || query.isBlank()) return Intent.GENERAL;
        String lower = query.toLowerCase();

        for (var entry : INTENT_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return Intent.GENERAL;
    }
}
