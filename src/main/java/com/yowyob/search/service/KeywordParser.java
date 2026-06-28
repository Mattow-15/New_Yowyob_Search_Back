package com.yowyob.search.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    /** Requête débarrassée des stopwords, villes et mots de proximité. */
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
            builder.append(part);
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
