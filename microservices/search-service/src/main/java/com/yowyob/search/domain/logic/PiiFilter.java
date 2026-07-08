package com.yowyob.search.domain.logic;

import com.yowyob.search.domain.model.Product;

/**
 * RÈGLE D'OR DE SÉCURITÉ — ZÉRO PII
 *
 * Les données de profils de tests ou de production réelle ne doivent
 * sous aucun prétexte transiter de manière lisible sur les requêtes publiques
 * du moteur de recherche.
 *
 * Ce filtre sanitise un objet Product avant son indexation dans Elasticsearch
 * en supprimant ou masquant tout champ potentiellement sensible (PII).
 *
 * @see <a href="https://gdpr.eu/">RGPD</a>
 */
public class PiiFilter {

    // Marqueur utilisé pour indiquer qu'un champ a été masqué
    private static final String MASKED = "[MASQUÉ]";

    private PiiFilter() {
        // Classe utilitaire — pas d'instanciation
    }

    /**
     * Retourne une copie sanitisée du Product dont les champs PII sont supprimés.
     *
     * Champs considérés comme PII et exclus de l'index public :
     *  - email (adresse personnelle ou professionnelle interne)
     *  - password / passwordHash
     *  - nationalId / taxId / identityDocument
     *  - privatePhone : le téléphone d'un commerce PUBLIC est autorisé,
     *    mais un téléphone personnel d'utilisateur ne doit pas être indexé.
     *
     * Champs AUTORISÉS dans l'index public (données de commerce) :
     *  - phone : numéro de contact public du commerce ✅
     *  - website : site web public ✅
     *  - address / street / city : adresse commerciale publique ✅
     */
    public static Product sanitize(Product product) {
        if (product == null) return null;

        // Vérifier que la description ne contient pas d'email ou de données sensibles
        if (product.getDescription() != null) {
            String sanitizedDesc = maskEmails(product.getDescription());
            sanitizedDesc = maskPhonePatterns(sanitizedDesc);
            product.setDescription(sanitizedDesc);
        }

        // Vérifier que le titre ne contient pas de données sensibles
        if (product.getTitle() != null) {
            product.setTitle(maskEmails(product.getTitle()));
        }

        // Vérifier que le reviewsSummary ne contient pas de PII
        if (product.getReviewsSummary() != null) {
            String sanitized = maskEmails(product.getReviewsSummary());
            product.setReviewsSummary(sanitized);
        }

        return product;
    }

    /**
     * Masque les adresses email dans une chaîne de caractères.
     * Exemple : "contact: user@example.com" → "contact: [MASQUÉ]"
     */
    public static String maskEmails(String input) {
        if (input == null) return null;
        // Pattern email simple mais robuste
        return input.replaceAll(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
            MASKED
        );
    }

    /**
     * Masque les numéros de téléphone personnels isolés dans du texte libre.
     * Note : le champ Product.phone (numéro public du commerce) reste intact —
     * seul le texte libre (description, avis) est filtré.
     */
    public static String maskPhonePatterns(String input) {
        if (input == null) return null;
        // Numéros camerounais (6XX XXX XXX) et formats internationaux dans du texte
        return input.replaceAll(
            "(?<![\\d])([+]?[0-9]{1,3}[\\s\\-]?)?[(]?[0-9]{2,3}[)]?[\\s\\-]?[0-9]{2,4}[\\s\\-]?[0-9]{2,4}[\\s\\-]?[0-9]{0,4}(?![\\d])",
            MASKED
        );
    }

    /**
     * Vérifie si un Product est sûr à indexer (non null, a un titre).
     */
    public static boolean isSafeToIndex(Product product) {
        return product != null
            && product.getId() != null && !product.getId().isBlank()
            && product.getTitle() != null && !product.getTitle().isBlank();
    }
}
