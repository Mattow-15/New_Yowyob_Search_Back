package com.yowyob.search.domain.logic;

import java.util.*;
import java.util.regex.Pattern;

public class KeywordParser {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "de", "des", "du", "la", "le", "les", "un", "une", "dans", "pour", "à", "a", "au", "aux",
            "je", "veux", "recherche", "trouver", "les", "l", "y", "en", "sur", "avec", "chez", "par",
            "se", "sa", "son", "ses", "mon", "ma", "mes", "want", "wanting",
            // Mots de proximité à exclure des résultats
            "pres", "proche", "autour", "loin", "tres", "moi"));

    private static final Set<String> CAMEROON_CITIES = new HashSet<>(Arrays.asList(
            "douala", "yaoundé", "yaounde", "buea", "bafoussam", "bamenda", "garoua",
            "limbe", "tiko", "kumba", "ngaoundere", "bertoua", "batouri", "ebolowa",
            "dschang", "foumban", "koutaba", "banyo", "nkongsamba", "edea", "kribi",
            "campo", "budenguangi"));

    private static final Map<String, Double> PROXIMITY_EXPRESSIONS = new HashMap<>();
    private static final Map<String, String> SYNONYMS = Map.of(
            "resto", "restaurant",
            "cafe", "restaurant",
            "tour", "agence",
            "lodge", "hotel",
            "auberge", "hotel");

    static {
        PROXIMITY_EXPRESSIONS.put("pres", 5.0); // "près de chez moi"
        PROXIMITY_EXPRESSIONS.put("proche", 5.0); // "proche de chez moi"
        PROXIMITY_EXPRESSIONS.put("autour", 5.0); // "autour de moi"
        PROXIMITY_EXPRESSIONS.put("a proximite", 7.0); // "à proximité"
        PROXIMITY_EXPRESSIONS.put("loin", 20.0); // "loin"
        PROXIMITY_EXPRESSIONS.put("tres loin", 50.0); // "très loin"
        PROXIMITY_EXPRESSIONS.put("tres pres", 2.0); // "très près"
    }

    private static final Map<String, String> CATEGORY_MAPPING = new HashMap<>();

    static {
        // --- IMMOBILIER / LOGEMENT ---
        String catImmo = "Immobilier";
        CATEGORY_MAPPING.put("immobilier", catImmo);
        CATEGORY_MAPPING.put("maison", catImmo);
        CATEGORY_MAPPING.put("logement", catImmo);
        CATEGORY_MAPPING.put("appartement", catImmo);
        CATEGORY_MAPPING.put("studio", catImmo);
        CATEGORY_MAPPING.put("chambre", catImmo);
        CATEGORY_MAPPING.put("villa", catImmo);
        CATEGORY_MAPPING.put("terrain", catImmo);
        CATEGORY_MAPPING.put("parcelle", catImmo);
        CATEGORY_MAPPING.put("louer", catImmo);
        CATEGORY_MAPPING.put("location", catImmo);
        CATEGORY_MAPPING.put("dormir", catImmo); // Intention: chercher un endroit où dormir
        CATEGORY_MAPPING.put("hotel", catImmo);
        CATEGORY_MAPPING.put("auberge", catImmo);
        CATEGORY_MAPPING.put("residence", catImmo);
        CATEGORY_MAPPING.put("duplex", catImmo);
        CATEGORY_MAPPING.put("voyage",catImmo);
        CATEGORY_MAPPING.put("voyages",catImmo);
        CATEGORY_MAPPING.put("tourisme",catImmo);
        CATEGORY_MAPPING.put("agence",catImmo);
        CATEGORY_MAPPING.put("hotel", catImmo);
        CATEGORY_MAPPING.put("vol",catImmo);

        // --- RESTAURATION / NOURRITURE ---
        String catResto = "Restaurant";
        CATEGORY_MAPPING.put("restaurant", catResto);
        CATEGORY_MAPPING.put("resto", catResto);
        CATEGORY_MAPPING.put("manger", catResto); // Intention
        CATEGORY_MAPPING.put("faim", catResto); // Intention
        CATEGORY_MAPPING.put("nourriture", catResto);
        CATEGORY_MAPPING.put("repas", catResto);
        CATEGORY_MAPPING.put("dejeuner", catResto);
        CATEGORY_MAPPING.put("diner", catResto);
        CATEGORY_MAPPING.put("snack", catResto);
        CATEGORY_MAPPING.put("pizza", catResto);
        CATEGORY_MAPPING.put("burger", catResto);
        CATEGORY_MAPPING.put("braise", catResto); // Poisson braisé etc.
        CATEGORY_MAPPING.put("poisson", catResto);
        CATEGORY_MAPPING.put("boire", catResto);
        CATEGORY_MAPPING.put("soif", catResto);
        CATEGORY_MAPPING.put("bar", catResto);
        CATEGORY_MAPPING.put("cafe", catResto);
        CATEGORY_MAPPING.put("boulangerie", catResto);

        // --- ELECTRONIQUE / MULTIMEDIA ---
        String catElec = "Electronique";
        CATEGORY_MAPPING.put("electronique", catElec);
        CATEGORY_MAPPING.put("telephone", catElec);
        CATEGORY_MAPPING.put("phone", catElec);
        CATEGORY_MAPPING.put("smartphone", catElec);
        CATEGORY_MAPPING.put("iphone", catElec);
        CATEGORY_MAPPING.put("samsung", catElec);
        CATEGORY_MAPPING.put("ordinateur", catElec);
        CATEGORY_MAPPING.put("pc", catElec);
        CATEGORY_MAPPING.put("laptop", catElec);
        CATEGORY_MAPPING.put("tablette", catElec);
        CATEGORY_MAPPING.put("tv", catElec);
        CATEGORY_MAPPING.put("ecran", catElec);

        // --- MODE / VETEMENTS ---
        String catMode = "Mode";
        CATEGORY_MAPPING.put("mode", catMode);
        CATEGORY_MAPPING.put("vetement", catMode);
        CATEGORY_MAPPING.put("habit", catMode);
        CATEGORY_MAPPING.put("chaussure", catMode);
        CATEGORY_MAPPING.put("robe", catMode);
        CATEGORY_MAPPING.put("pantalon", catMode);
        CATEGORY_MAPPING.put("chemise", catMode);
        CATEGORY_MAPPING.put("sac", catMode);
        CATEGORY_MAPPING.put("bijou", catMode);
        CATEGORY_MAPPING.put("montre", catMode);

        // --- BEAUTE / BIEN-ETRE ---
        String catBeauty = "Beaute";
        CATEGORY_MAPPING.put("beaute", catBeauty);
        CATEGORY_MAPPING.put("coiffure", catBeauty);
        CATEGORY_MAPPING.put("salon", catBeauty);
        CATEGORY_MAPPING.put("soin", catBeauty);
        CATEGORY_MAPPING.put("massage", catBeauty);
        CATEGORY_MAPPING.put("onglerie", catBeauty);
        CATEGORY_MAPPING.put("maquillage", catBeauty);
        CATEGORY_MAPPING.put("parfum", catBeauty);

        // --- TRANSPORT / AUTOMOBILE ---
        String catAuto = "Automobile";
        CATEGORY_MAPPING.put("voiture", catAuto);
        CATEGORY_MAPPING.put("auto", catAuto);
        CATEGORY_MAPPING.put("vehicule", catAuto);
        CATEGORY_MAPPING.put("taxi", catAuto);
        CATEGORY_MAPPING.put("transport", catAuto);
        CATEGORY_MAPPING.put("location de voiture", catAuto);
        CATEGORY_MAPPING.put("mecano", catAuto);
        CATEGORY_MAPPING.put("garage", catAuto);

        // --- SERVICES / AGENCES ---
        String catService = "Services";
        CATEGORY_MAPPING.put("plombier", catService);
        CATEGORY_MAPPING.put("electricien", catService);
        CATEGORY_MAPPING.put("nettoyage", catService);
        CATEGORY_MAPPING.put("gardiennage", catService);
        CATEGORY_MAPPING.put("demenagement", catService);
        CATEGORY_MAPPING.put("agence", catService);
        CATEGORY_MAPPING.put("voyage", catService);
    }

    private static final Pattern NON_LETTER_DIGIT = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    public static class ParsedQueryResult {
        public String query;
        public String extractedCity;
        public String inferredCategory; // Catégorie déduite sémantiquement
        public Double proximityRadius; // Rayon de proximité en km
        public boolean isProximitySearch; // Indique si c'est une recherche de proximité

        public ParsedQueryResult(String query, String extractedCity) {
            this(query, extractedCity, null, null, false);
        }

        public ParsedQueryResult(String query, String extractedCity, String inferredCategory, Double proximityRadius,
                boolean isProximitySearch) {
            this.query = query;
            this.extractedCity = extractedCity;
            this.inferredCategory = inferredCategory;
            this.proximityRadius = proximityRadius;
            this.isProximitySearch = isProximitySearch;
        }
    }

    public List<String> extractKeywords(String input) {
        if (input == null)
            return Collections.emptyList();
        String cleaned = input.trim().toLowerCase();
        cleaned = cleaned.replace("'", "'");
        cleaned = NON_LETTER_DIGIT.matcher(cleaned).replaceAll(" ");
        String[] parts = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty())
                continue;
            if (STOPWORDS.contains(p))
                continue;
            if (CAMEROON_CITIES.contains(p))
                continue;
            String singular = singularize(p);
            String token = SYNONYMS.getOrDefault(singular, singular);
            tokens.add(token);
        }
        return tokens;
    }

    private String singularize(String s) {
        if (s.length() > 3 && s.endsWith("s")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    public String buildQuery(String input) {
        List<String> keys = extractKeywords(input);
        if (keys.isEmpty()) {
            return (input == null) ? "" : input.trim();
        }
        return String.join(" ", keys);
    }

    public String extractCity(String input) {
        if (input == null)
            return null;
        String cleaned = input.trim().toLowerCase();
        cleaned = cleaned.replace("'", "'");
        cleaned = NON_LETTER_DIGIT.matcher(cleaned).replaceAll(" ");
        String[] parts = cleaned.split("\\s+");

        for (String part : parts) {
            if (CAMEROON_CITIES.contains(part)) {
                return part.substring(0, 1).toUpperCase() + part.substring(1);
            }
        }
        return null;
    }

    public ParsedQueryResult parseWithCity(String input) {
        String parsedQuery = buildQuery(input);
        String city = extractCity(input);
        String category = extractCategory(input);

        Double proximityRadius = extractProximityRadius(input);
        boolean isProximitySearch = proximityRadius != null;

        return new ParsedQueryResult(parsedQuery, city, category, proximityRadius, isProximitySearch);
    }

    public String extractCategory(String input) {
        if (input == null)
            return null;
        List<String> keywords = extractKeywords(input);

        for (String keyword : keywords) {
            if (CATEGORY_MAPPING.containsKey(keyword)) {
                return CATEGORY_MAPPING.get(keyword);
            }
            for (String cat : CATEGORY_MAPPING.values()) {
                if (cat.equalsIgnoreCase(keyword)) {
                    return cat;
                }
            }
        }
        return null;
    }

    public Double extractProximityRadius(String input) {
        if (input == null)
            return null;
        String cleaned = input.trim().toLowerCase();
        cleaned = cleaned.replace("'", "'");
        cleaned = NON_LETTER_DIGIT.matcher(cleaned).replaceAll(" ");

        if (cleaned.contains("tres") && cleaned.contains("loin")) {
            return 50.0;
        }
        if (cleaned.contains("tres") && cleaned.contains("pres")) {
            return 2.0;
        }
        if (cleaned.contains("proximite")) {
            return 7.0;
        }
        if (cleaned.contains("pres") && (cleaned.contains("chez") || cleaned.contains("moi"))) {
            return 5.0;
        }
        if (cleaned.contains("proche")) {
            return 5.0;
        }
        if (cleaned.contains("autour")) {
            return 5.0;
        }
        if (cleaned.contains("loin")) {
            return 20.0;
        }

        return null;
    }
}
