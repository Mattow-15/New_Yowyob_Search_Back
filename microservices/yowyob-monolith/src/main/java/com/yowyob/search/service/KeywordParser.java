/**
 * Parser for user search queries.
 * Extracts keywords, cities, categories, and proximity expressions
 * from natural language input in French and English.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.search.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class KeywordParser {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "de", "des", "du", "la", "le", "les", "un", "une", "dans", "pour", "à", "a", "au", "aux",
            "je", "veux", "recherche", "trouver", "les", "l", "y", "en", "sur", "avec", "chez", "par",
            "se", "sa", "son", "ses", "mon", "ma", "mes", "want", "wanting",
            // Mots de proximité à exclure des résultats
            "pres", "proche", "autour", "loin", "tres", "moi"));

    // Synonymes pour améliorer la recherche
    // NOTE: Les synonymes sont maintenant gérés par Elasticsearch via l'analyzer
    // Ce code en Java reste pour reference mais n'est plus nécessaire
    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        // Les synonymes sont gérés à la source dans Elasticsearch
        // setup_synonyms_index.sh contient la liste complète
    }

    private static final Set<String> CAMEROON_CITIES = new HashSet<>(Arrays.asList(
            "douala", "yaoundé", "yaounde", "buea", "bafoussam", "bamenda", "garoua",
            "limbe", "tiko", "kumba", "ngaoundere", "bertoua", "batouri", "ebolowa",
            "dschang", "foumban", "koutaba", "banyo", "nkongsamba", "edea", "kribi",
            "campo", "budenguangi"));

    // Expressions de proximité et leurs rayons en km
    private static final Map<String, Double> PROXIMITY_EXPRESSIONS = new HashMap<>();

    static {
        PROXIMITY_EXPRESSIONS.put("pres", 5.0); // "près de chez moi"
        PROXIMITY_EXPRESSIONS.put("proche", 5.0); // "proche de chez moi"
        PROXIMITY_EXPRESSIONS.put("autour", 5.0); // "autour de moi"
        PROXIMITY_EXPRESSIONS.put("a proximite", 7.0); // "à proximité"
        PROXIMITY_EXPRESSIONS.put("loin", 20.0); // "loin"
        PROXIMITY_EXPRESSIONS.put("tres loin", 50.0); // "très loin"
        PROXIMITY_EXPRESSIONS.put("tres pres", 2.0); // "très près"
    }

    // Mapping sémantique mots-clés -> Catégories
    private static final Map<String, String> CATEGORY_MAPPING = new HashMap<>();

    static {
        // --- IMMOBILIER / LOGEMENT ---
        String cat_immo = "Immobilier";
        CATEGORY_MAPPING.put("immobilier", cat_immo);
        CATEGORY_MAPPING.put("maison", cat_immo);
        CATEGORY_MAPPING.put("logement", cat_immo);
        CATEGORY_MAPPING.put("appartement", cat_immo);
        CATEGORY_MAPPING.put("studio", cat_immo);
        CATEGORY_MAPPING.put("chambre", cat_immo);
        CATEGORY_MAPPING.put("villa", cat_immo);
        CATEGORY_MAPPING.put("terrain", cat_immo);
        CATEGORY_MAPPING.put("parcelle", cat_immo);
        CATEGORY_MAPPING.put("louer", cat_immo);
        CATEGORY_MAPPING.put("location", cat_immo);
        CATEGORY_MAPPING.put("dormir", cat_immo); // Intention: chercher un endroit où dormir
        CATEGORY_MAPPING.put("hotel", cat_immo);
        CATEGORY_MAPPING.put("auberge", cat_immo);
        CATEGORY_MAPPING.put("residence", cat_immo);
        CATEGORY_MAPPING.put("duplex", cat_immo);
        CATEGORY_MAPPING.put("voyage", cat_immo);
        CATEGORY_MAPPING.put("voyages", cat_immo);
        CATEGORY_MAPPING.put("tourisme", cat_immo);
        CATEGORY_MAPPING.put("agence", cat_immo);
        CATEGORY_MAPPING.put("vol", cat_immo);

        // --- RESTAURATION / NOURRITURE ---
        String cat_resto = "Restaurant";
        CATEGORY_MAPPING.put("restaurant", cat_resto);
        CATEGORY_MAPPING.put("resto", cat_resto);
        CATEGORY_MAPPING.put("manger", cat_resto); // Intention
        CATEGORY_MAPPING.put("faim", cat_resto); // Intention
        CATEGORY_MAPPING.put("nourriture", cat_resto);
        CATEGORY_MAPPING.put("repas", cat_resto);
        CATEGORY_MAPPING.put("dejeuner", cat_resto);
        CATEGORY_MAPPING.put("diner", cat_resto);
        CATEGORY_MAPPING.put("snack", cat_resto);
        CATEGORY_MAPPING.put("pizza", cat_resto);
        CATEGORY_MAPPING.put("burger", cat_resto);
        CATEGORY_MAPPING.put("braise", cat_resto); // Poisson braisé etc.
        CATEGORY_MAPPING.put("poisson", cat_resto);
        CATEGORY_MAPPING.put("boire", cat_resto);
        CATEGORY_MAPPING.put("soif", cat_resto);
        CATEGORY_MAPPING.put("bar", cat_resto);
        CATEGORY_MAPPING.put("cafe", cat_resto);
        CATEGORY_MAPPING.put("boulangerie", cat_resto);

        // --- ELECTRONIQUE / MULTIMEDIA ---
        String cat_elec = "Electronique";
        CATEGORY_MAPPING.put("electronique", cat_elec);
        CATEGORY_MAPPING.put("telephone", cat_elec);
        CATEGORY_MAPPING.put("phone", cat_elec);
        CATEGORY_MAPPING.put("smartphone", cat_elec);
        CATEGORY_MAPPING.put("iphone", cat_elec);
        CATEGORY_MAPPING.put("samsung", cat_elec);
        CATEGORY_MAPPING.put("ordinateur", cat_elec);
        CATEGORY_MAPPING.put("pc", cat_elec);
        CATEGORY_MAPPING.put("laptop", cat_elec);
        CATEGORY_MAPPING.put("tablette", cat_elec);
        CATEGORY_MAPPING.put("tv", cat_elec);
        CATEGORY_MAPPING.put("ecran", cat_elec);

        // --- MODE / VETEMENTS ---
        String cat_mode = "Mode";
        CATEGORY_MAPPING.put("mode", cat_mode);
        CATEGORY_MAPPING.put("vetement", cat_mode);
        CATEGORY_MAPPING.put("habit", cat_mode);
        CATEGORY_MAPPING.put("chaussure", cat_mode);
        CATEGORY_MAPPING.put("robe", cat_mode);
        CATEGORY_MAPPING.put("pantalon", cat_mode);
        CATEGORY_MAPPING.put("chemise", cat_mode);
        CATEGORY_MAPPING.put("sac", cat_mode);
        CATEGORY_MAPPING.put("bijou", cat_mode);
        CATEGORY_MAPPING.put("montre", cat_mode);

        // --- BEAUTE / BIEN-ETRE ---
        String cat_beauty = "Beaute";
        CATEGORY_MAPPING.put("beaute", cat_beauty);
        CATEGORY_MAPPING.put("coiffure", cat_beauty);
        CATEGORY_MAPPING.put("salon", cat_beauty);
        CATEGORY_MAPPING.put("soin", cat_beauty);
        CATEGORY_MAPPING.put("massage", cat_beauty);
        CATEGORY_MAPPING.put("onglerie", cat_beauty);
        CATEGORY_MAPPING.put("maquillage", cat_beauty);
        CATEGORY_MAPPING.put("parfum", cat_beauty);

        // --- TRANSPORT / AUTOMOBILE ---
        String cat_auto = "Automobile";
        CATEGORY_MAPPING.put("voiture", cat_auto);
        CATEGORY_MAPPING.put("auto", cat_auto);
        CATEGORY_MAPPING.put("vehicule", cat_auto);
        CATEGORY_MAPPING.put("taxi", cat_auto);
        CATEGORY_MAPPING.put("transport", cat_auto);
        CATEGORY_MAPPING.put("location de voiture", cat_auto);
        CATEGORY_MAPPING.put("mecano", cat_auto);
        CATEGORY_MAPPING.put("garage", cat_auto);

        // --- SERVICES / AGENCES ---
        String cat_service = "Services";
        CATEGORY_MAPPING.put("plombier", cat_service);
        CATEGORY_MAPPING.put("electricien", cat_service);
        CATEGORY_MAPPING.put("nettoyage", cat_service);
        CATEGORY_MAPPING.put("gardiennage", cat_service);
        CATEGORY_MAPPING.put("demenagement", cat_service);
    }

    private static final Pattern NON_LETTER_DIGIT = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    /**
     * Inner class representing the result of query parsing.
     * Contains the parsed query, extracted city, inferred category,
     * and proximity search parameters.
     */
    public class ParsedQueryResult {
        public String query;
        public String extracted_city;
        public String inferred_category;
        public Double proximity_radius;
        public boolean is_proximity_search;

        /**
         * Creates a simple parsed result with query and city only.
         *
         * @param query          the parsed search query
         * @param extracted_city the city extracted from user input
         */
        public ParsedQueryResult(String query, String extracted_city) {
            this(query, extracted_city, null, null, false);
        }

        /**
         * Creates a full parsed result with all parameters.
         *
         * @param query               the parsed search query
         * @param extracted_city      the city extracted from user input
         * @param inferred_category   the category inferred from keywords
         * @param proximity_radius    the proximity radius in km
         * @param is_proximity_search whether this is a proximity search
         */
        public ParsedQueryResult(String query, String extracted_city, String inferred_category,
                Double proximity_radius, boolean is_proximity_search) {
            this.query = query;
            this.extracted_city = extracted_city;
            this.inferred_category = inferred_category;
            this.proximity_radius = proximity_radius;
            this.is_proximity_search = is_proximity_search;
        }
    }

    /**
     * Extracts meaningful keywords from user input by removing stopwords,
     * city names, and applying singularization and synonym resolution.
     *
     * @param input the raw user search query
     * @return list of extracted keywords
     */
    public List<String> extractKeywords(String input) {
        if (input == null)
            return Collections.emptyList();
        String cleaned = input.trim().toLowerCase();
        cleaned = cleaned.replace("\u2019", "'");
        // remplace les caractères non lettres/chiffres par un espace
        cleaned = NON_LETTER_DIGIT.matcher(cleaned).replaceAll(" ");
        String[] parts = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty())
                continue;
            if (STOPWORDS.contains(p))
                continue;
            // Ne pas inclure les noms de villes dans les mots-clés
            // (seront gérés séparément)
            if (CAMEROON_CITIES.contains(p))
                continue;
            String token = singularize(p);
            // Appliquer les synonymes
            token = resolveSynonym(token);
            tokens.add(token);
        }
        return tokens;
    }

    /**
     * Simple heuristic to singularize a French word by removing trailing 's'.
     *
     * @param s the word to singularize
     * @return the singularized form
     */
    private String singularize(String s) {
        // simple heuristique: si mot > 3 lettres et termine par 's', on retire le 's'
        if (s.length() > 3 && s.endsWith("s")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Résout un mot à son terme de recherche canonique via les synonymes.
     * Par exemple: "resto" → "restaurant", "cafe" → "restaurant"
     *
     * @param word Le mot à résoudre
     * @return Le terme canonical ou le mot original si pas de synonyme
     */
    private String resolveSynonym(String word) {
        // Chercher le mot exact dans les synonymes
        if (SYNONYMS.containsKey(word)) {
            return SYNONYMS.get(word);
        }

        // Si pas de match exact, retourner le mot original
        return word;
    }

    /**
     * Builds a clean search query string from extracted keywords.
     *
     * @param input the raw user search query
     * @return the processed query string
     */
    public String buildQuery(String input) {
        List<String> keys = extractKeywords(input);
        if (keys.isEmpty()) {
            return (input == null) ? "" : input.trim();
        }
        return String.join(" ", keys);
    }

    /**
     * Extrait une ville du Cameroun de la requête.
     *
     * @param input Requête utilisateur
     * @return Nom de la ville trouvée, ou null si aucune ville
     */
    public String extractCity(String input) {
        if (input == null)
            return null;
        String cleaned = input.trim().toLowerCase();
        cleaned = cleaned.replace("\u2019", "'");
        cleaned = NON_LETTER_DIGIT.matcher(cleaned).replaceAll(" ");
        String[] parts = cleaned.split("\\s+");

        for (String part : parts) {
            if (CAMEROON_CITIES.contains(part)) {
                // Retourner le nom de la ville avec la première lettre en majuscule
                return part.substring(0, 1).toUpperCase() + part.substring(1);
            }
        }
        return null;
    }

    /**
     * Parse la requête et extrait à la fois les mots-clés et la ville.
     *
     * @param input Requête utilisateur
     * @return ParsedQueryResult contenant la requête parsée et la ville extraite
     */
    public ParsedQueryResult parseWithCity(String input) {
        String parsed_query = buildQuery(input);
        String city = extractCity(input);
        String category = extractCategory(input);

        // Vérifier si c'est une recherche de proximité
        Double proximity_radius = extractProximityRadius(input);
        boolean is_proximity_search = proximity_radius != null;

        return new ParsedQueryResult(parsed_query, city, category, proximity_radius, is_proximity_search);
    }

    /**
     * Déduit une catégorie basée sur les mots-clés de la requête.
     *
     * @param input Requête utilisateur
     * @return Catégorie déduite (ex: "Immobilier", "Restaurant") ou null
     */
    public String extractCategory(String input) {
        if (input == null)
            return null;
        List<String> keywords = extractKeywords(input);

        // Parcourir les mots-clés pour trouver une correspondance de catégorie
        for (String keyword : keywords) {
            // Vérification directe
            if (CATEGORY_MAPPING.containsKey(keyword)) {
                return CATEGORY_MAPPING.get(keyword);
            }
            // Vérification des valeurs (si l'utilisateur tape "Beauty" directement)
            for (String cat : CATEGORY_MAPPING.values()) {
                if (cat.equalsIgnoreCase(keyword)) {
                    return cat;
                }
            }
        }
        return null;
    }

    /**
     * Extrait le rayon de proximité des expressions comme "près de chez moi",
     * "loin", etc.
     *
     * @param input Requête utilisateur
     * @return Rayon en km, ou null si pas d'expression de proximité
     */
    public Double extractProximityRadius(String input) {
        if (input == null)
            return null;
        String cleaned = input.trim().toLowerCase();
        cleaned = cleaned.replace("\u2019", "'");
        cleaned = NON_LETTER_DIGIT.matcher(cleaned).replaceAll(" ");

        // Chercher les patterns spécifiques
        if (cleaned.contains("tres") && cleaned.contains("loin")) {
            return 50.0; // "très loin" → 50km
        }
        if (cleaned.contains("tres") && cleaned.contains("pres")) {
            return 2.0; // "très près" → 2km
        }
        if (cleaned.contains("proximite")) {
            return 7.0; // "à proximité" → 7km
        }
        if (cleaned.contains("pres") && (cleaned.contains("chez") || cleaned.contains("moi"))) {
            return 5.0; // "près de chez moi" → 5km
        }
        if (cleaned.contains("proche")) {
            return 5.0; // "proche" → 5km
        }
        if (cleaned.contains("autour")) {
            return 5.0; // "autour" → 5km
        }
        if (cleaned.contains("loin")) {
            return 20.0; // "loin" → 20km
        }

        return null;
    }
}