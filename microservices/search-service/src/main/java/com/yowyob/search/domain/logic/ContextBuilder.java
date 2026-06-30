package com.yowyob.search.domain.logic;

import com.yowyob.search.domain.model.Product;
import java.util.List;

public class ContextBuilder {

    public String build(List<Product> documents, String query) {
        if (documents == null || documents.isEmpty()) {
            return "Aucun commerce trouvé pour cette recherche.";
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("Requête utilisateur : ").append(query).append("\n\n");
        ctx.append("Commerces disponibles dans la base YowYob :\n\n");

        int i = 1;
        for (Product doc : documents.stream().limit(5).toList()) {
            ctx.append(i++).append(". **").append(doc.getTitle()).append("**\n");

            if (doc.getCategory() != null)
                ctx.append("   Type : ").append(doc.getCategory()).append("\n");

            if (doc.getCity() != null)
                ctx.append("   Ville : ").append(doc.getCity()).append("\n");

            if (doc.getStreet() != null)
                ctx.append("   Rue : ").append(doc.getStreet()).append("\n");

            if (doc.getPhone() != null)
                ctx.append("   Téléphone : ").append(doc.getPhone()).append("\n");

            if (doc.getWebsite() != null)
                ctx.append("   Site web : ").append(doc.getWebsite()).append("\n");

            if (doc.getRating() != null)
                ctx.append("   Note : ").append(doc.getRating()).append("/5");
            if (doc.getReviewsCount() != null)
                ctx.append(" (").append(doc.getReviewsCount()).append(" avis)");
            if (doc.getRating() != null) ctx.append("\n");

            if (doc.getOpenNow() != null)
                ctx.append("   Statut : ")
                   .append(doc.getOpenNow() ? "Ouvert maintenant" : "Fermé")
                   .append("\n");

            if (doc.getOpeningHours() != null) {
                ctx.append("   Horaires : ")
                   .append(doc.getOpeningHours(), 0,
                       Math.min(doc.getOpeningHours().length(), 100))
                   .append("\n");
            }

            if (doc.getPriceLevel() != null)
                ctx.append("   Prix : ")
                   .append("$".repeat(Math.min(doc.getPriceLevel() + 1, 4)))
                   .append("\n");

            if (doc.getDescription() != null) {
                ctx.append("   Description : ")
                   .append(doc.getDescription(), 0,
                       Math.min(doc.getDescription().length(), 150))
                   .append("...\n");
            }

            if (doc.getReviewsSummary() != null) {
                String firstReview = doc.getReviewsSummary().split("\\|\\|")[0].trim();
                ctx.append("   Avis client : ").append(firstReview).append("\n");
            }

            ctx.append("\n");
        }

        return ctx.toString();
    }

    public String buildPrompt(String context,
                               String query,
                               IntentDetector.Intent intent) {
        String instruction = switch (intent) {
            case RECOMMENDATION ->
                """
                Tu es un assistant local camerounais expert en commerces et services.
                Basé UNIQUEMENT sur les données ci-dessous, recommande les meilleures
                options en expliquant pourquoi chacune est pertinente.
                Classe-les du meilleur au moins bon selon la note et les avis.
                Si une information n'est pas dans les données, ne l'invente PAS.
                """;
            case INFORMATION ->
                """
                Tu es un assistant local camerounais.
                Réponds à la question de l'utilisateur en te basant UNIQUEMENT
                sur les informations fournies ci-dessous.
                Sois précis, concis et factuel.
                Si l'information demandée n'est pas disponible, dis-le clairement.
                """;
            case NAVIGATION ->
                """
                Tu es un assistant local camerounais.
                Fournis les informations de localisation disponibles pour aider
                l'utilisateur à trouver le lieu.
                Inclus l'adresse, le quartier et le téléphone si disponibles.
                """;
            default ->
                """
                Tu es un assistant local camerounais expert en commerces et services.
                Réponds à la question de l'utilisateur basé UNIQUEMENT sur les données
                fournies. Ne jamais inventer d'informations.
                Réponds en français, de façon claire et utile.
                """;
        };

        return instruction + "\n\n" + context + "\n\nQuestion : " + query;
    }
}
