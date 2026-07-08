package com.yowyob.search.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Analyse la requête en langage naturel (FR/EN) pour en extraire :
 * <ul>
 *   <li>une <b>ville</b> camerounaise éventuelle (pour la recherche de proximité) ;</li>
 *   <li>une <b>intention de proximité</b> (« près de chez moi », « autour », « loin »…) et son rayon ;</li>
 *   <li>la requête <b>nettoyée</b> des stopwords/villes/mots de proximité (utilisée pour le lexical).</li>
 * </ul>
 *
 * Service de recherche générique : on ne suppose pas de catalogue produit. Ville + proximité sont
 * les signaux réellement exploités par {@code SearchService}.
 */
@Component
public class KeywordParser {

    private static final Pattern NON_LETTER_DIGIT = Pattern.compile("[^\\p{L}\\p{Nd}]+");

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "de", "des", "du", "la", "le", "les", "un", "une", "dans", "pour", "a", "au", "aux",
            "je", "veux", "recherche", "trouver", "l", "y", "en", "sur", "avec", "chez", "par",
            "se", "sa", "son", "ses", "mon", "ma", "mes", "want", "wanting", "near", "around",
            "pres", "proche", "autour", "loin", "tres", "moi"));

    /**
     * Intentions courantes → termes de recherche ES.
     * Permet de trouver des résultats même quand la requête ne contient aucun mot-clé littéral
     * présent dans les titres/catégories indexées (ex. "manger" → "restaurant").
     */
    private static final Map<String, String> INTENT_SYNONYMS = new LinkedHashMap<>();
    static {
        // Nourriture
        INTENT_SYNONYMS.put("manger", "restaurant");
        INTENT_SYNONYMS.put("faim", "restaurant");
        INTENT_SYNONYMS.put("dejeuner", "restaurant");
        INTENT_SYNONYMS.put("diner", "restaurant");
        INTENT_SYNONYMS.put("grignoter", "restaurant snack");
        INTENT_SYNONYMS.put("nourriture", "restaurant");
        INTENT_SYNONYMS.put("repas", "restaurant");
        INTENT_SYNONYMS.put("miam", "restaurant");
        // Boissons
        INTENT_SYNONYMS.put("boire", "bar");
        INTENT_SYNONYMS.put("soif", "bar");
        INTENT_SYNONYMS.put("biere", "bar");
        INTENT_SYNONYMS.put("boisson", "bar");
        // Sport
        INTENT_SYNONYMS.put("foot", "football terrain stade");
        INTENT_SYNONYMS.put("football", "football terrain stade");
        INTENT_SYNONYMS.put("jouer", "terrain sport");
        INTENT_SYNONYMS.put("sport", "terrain stade sport");
        INTENT_SYNONYMS.put("gym", "salle sport fitness");
        INTENT_SYNONYMS.put("fitness", "salle sport gym");
        // Santé
        INTENT_SYNONYMS.put("medicament", "pharmacie");
        INTENT_SYNONYMS.put("ordonnance", "pharmacie");
        INTENT_SYNONYMS.put("malade", "clinique hopital");
        INTENT_SYNONYMS.put("soin", "clinique hopital");
        INTENT_SYNONYMS.put("docteur", "clinique hopital medecin");
        INTENT_SYNONYMS.put("medecin", "clinique hopital");
        // Argent / banque
        INTENT_SYNONYMS.put("argent", "banque atm distributeur");
        INTENT_SYNONYMS.put("retirer", "atm distributeur banque");
        INTENT_SYNONYMS.put("transfert", "banque mobile money");
        // Commerce
        INTENT_SYNONYMS.put("acheter", "magasin marche boutique");
        INTENT_SYNONYMS.put("courses", "supermarche marche");
        INTENT_SYNONYMS.put("shopping", "boutique magasin centre commercial");
        // Carburant / transport
        INTENT_SYNONYMS.put("essence", "station service carburant");
        INTENT_SYNONYMS.put("carburant", "station service essence");
        INTENT_SYNONYMS.put("taxi", "taxi transport");
        // Hébergement
        INTENT_SYNONYMS.put("dormir", "hotel");
        INTENT_SYNONYMS.put("nuit", "hotel");
        INTENT_SYNONYMS.put("loger", "hotel");
    }

    private static final Set<String> CITIES = new HashSet<>(Arrays.asList(
            "douala", "yaounde", "buea", "bafoussam", "bamenda", "garoua", "maroua",
            "limbe", "tiko", "kumba", "ngaoundere", "bertoua", "ebolowa",
            "dschang", "foumban", "nkongsamba", "edea", "kribi"));

    /** Résultat de l'analyse d'une requête. */
    public record ParsedQuery(String query, String city, Double proximityRadiusKm, boolean proximity) {
    }

    public ParsedQuery parse(String input) {
        String cleanedQuery = buildQuery(input);
        String city = extractCity(input);
        Double radius = extractProximityRadius(input);
        return new ParsedQuery(cleanedQuery, city, radius, radius != null);
    }

    /** Requête débarrassée des stopwords/villes, avec expansion des intentions courantes. */
    public String buildQuery(String input) {
        if (input == null) {
            return "";
        }
        String[] parts = tokens(input);
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || STOPWORDS.contains(part) || CITIES.contains(part)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String expanded = INTENT_SYNONYMS.get(part);
            builder.append(expanded != null ? expanded : part);
        }
        String result = builder.toString().trim();
        return result.isEmpty() ? input.trim() : result;
    }

    /** Ville camerounaise détectée (capitalisée) ou null. */
    public String extractCity(String input) {
        if (input == null) {
            return null;
        }
        for (String part : tokens(input)) {
            if (CITIES.contains(part)) {
                return part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1);
            }
        }
        return null;
    }

    /** Rayon (km) déduit d'une expression de proximité, ou null si aucune. */
    public Double extractProximityRadius(String input) {
        if (input == null) {
            return null;
        }
        String cleaned = normalize(input);
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
        if (cleaned.contains("proche") || cleaned.contains("autour") || cleaned.contains("around")
                || cleaned.contains("near")) {
            return 5.0;
        }
        if (cleaned.contains("loin")) {
            return 20.0;
        }
        return null;
    }

    private static String[] tokens(String input) {
        return NON_LETTER_DIGIT.matcher(normalize(input)).replaceAll(" ").split("\\s+");
    }

    private static String normalize(String input) {
        String lowered = input.trim().toLowerCase(Locale.ROOT).replace('’', '\'');
        return Normalizer.normalize(lowered, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    /** Liste des villes connues (utile pour la documentation / les tests). */
    public List<String> knownCities() {
        return List.copyOf(CITIES);
    }
}
