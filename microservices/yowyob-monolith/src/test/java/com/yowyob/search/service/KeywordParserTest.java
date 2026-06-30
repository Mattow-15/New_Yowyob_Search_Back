/**
 * Unit tests for KeywordParser.
 * Tests keyword extraction, city extraction, category inference,
 * and proximity radius detection from user queries.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KeywordParserTest {

    private KeywordParser keyword_parser;

    @BeforeEach
    void setUp() {
        keyword_parser = new KeywordParser();
    }

    // ===== extractKeywords tests =====

    @Test
    @DisplayName("extractKeywords - should return empty list for null input")
    void extractKeywords_null_input_returns_empty() {
        List<String> result = keyword_parser.extractKeywords(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractKeywords - should return empty list for empty input")
    void extractKeywords_empty_input_returns_empty() {
        List<String> result = keyword_parser.extractKeywords("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extractKeywords - should remove stopwords")
    void extractKeywords_removes_stopwords() {
        List<String> result = keyword_parser.extractKeywords("je veux trouver un restaurant");
        // "je", "veux", "trouver", "un" are stopwords, "restaurant" should remain
        assertTrue(result.contains("restaurant"));
        assertFalse(result.contains("je"));
        assertFalse(result.contains("veux"));
        assertFalse(result.contains("trouver"));
        assertFalse(result.contains("un"));
    }

    @Test
    @DisplayName("extractKeywords - should remove city names from keywords")
    void extractKeywords_removes_cities() {
        List<String> result = keyword_parser.extractKeywords("restaurant douala");
        assertTrue(result.contains("restaurant"));
        assertFalse(result.contains("douala"));
    }

    @Test
    @DisplayName("extractKeywords - should singularize words ending in s")
    void extractKeywords_singularizes() {
        List<String> result = keyword_parser.extractKeywords("restaurants");
        // "restaurants" has > 3 chars and ends with 's', so should become "restaurant"
        assertTrue(result.contains("restaurant"));
        assertFalse(result.contains("restaurants"));
    }

    @Test
    @DisplayName("extractKeywords - should not singularize short words")
    void extractKeywords_no_singularize_short() {
        List<String> result = keyword_parser.extractKeywords("bus");
        // "bus" has 3 chars, should not be singularized
        assertTrue(result.contains("bus"));
    }

    @Test
    @DisplayName("extractKeywords - should handle accented characters")
    void extractKeywords_handles_accents() {
        List<String> result = keyword_parser.extractKeywords("hôtel à yaoundé");
        // "à" is a stopword, "yaoundé" is a city
        assertTrue(result.contains("hôtel") || result.contains("hotel"));
        assertFalse(result.contains("yaoundé"));
    }

    // ===== extractCity tests =====

    @Test
    @DisplayName("extractCity - should return null for null input")
    void extractCity_null_input() {
        String result = keyword_parser.extractCity(null);
        assertNull(result);
    }

    @Test
    @DisplayName("extractCity - should extract Douala")
    void extractCity_extracts_douala() {
        String result = keyword_parser.extractCity("restaurant à douala");
        assertEquals("Douala", result);
    }

    @Test
    @DisplayName("extractCity - should extract Yaoundé with accent")
    void extractCity_extracts_yaounde_accent() {
        String result = keyword_parser.extractCity("hotel yaoundé");
        assertEquals("Yaoundé", result);
    }

    @Test
    @DisplayName("extractCity - should extract Buea")
    void extractCity_extracts_buea() {
        String result = keyword_parser.extractCity("apartment in buea");
        assertEquals("Buea", result);
    }

    @Test
    @DisplayName("extractCity - should return null when no city found")
    void extractCity_no_city() {
        String result = keyword_parser.extractCity("restaurant pas cher");
        assertNull(result);
    }

    // ===== extractCategory tests =====

    @Test
    @DisplayName("extractCategory - should return null for null input")
    void extractCategory_null_input() {
        String result = keyword_parser.extractCategory(null);
        assertNull(result);
    }

    @Test
    @DisplayName("extractCategory - should detect Restaurant category")
    void extractCategory_detects_restaurant() {
        String result = keyword_parser.extractCategory("je veux manger");
        assertEquals("Restaurant", result);
    }

    @Test
    @DisplayName("extractCategory - should detect Immobilier category")
    void extractCategory_detects_immobilier() {
        String result = keyword_parser.extractCategory("louer un appartement");
        assertEquals("Immobilier", result);
    }

    @Test
    @DisplayName("extractCategory - should detect Electronique category")
    void extractCategory_detects_electronique() {
        String result = keyword_parser.extractCategory("acheter un iphone");
        assertEquals("Electronique", result);
    }

    @Test
    @DisplayName("extractCategory - should return null when no category found")
    void extractCategory_no_category() {
        String result = keyword_parser.extractCategory("bonjour");
        assertNull(result);
    }

    // ===== extractProximityRadius tests =====

    @Test
    @DisplayName("extractProximityRadius - should return null for null input")
    void extractProximityRadius_null_input() {
        Double result = keyword_parser.extractProximityRadius(null);
        assertNull(result);
    }

    @Test
    @DisplayName("extractProximityRadius - should detect 'tres loin' as 50km")
    void extractProximityRadius_tres_loin() {
        Double result = keyword_parser.extractProximityRadius("restaurant tres loin");
        assertNotNull(result);
        assertEquals(50.0, result);
    }

    @Test
    @DisplayName("extractProximityRadius - should detect 'tres pres' as 2km")
    void extractProximityRadius_tres_pres() {
        Double result = keyword_parser.extractProximityRadius("restaurant tres pres de moi");
        assertNotNull(result);
        assertEquals(2.0, result);
    }

    @Test
    @DisplayName("extractProximityRadius - should detect 'proche' as 5km")
    void extractProximityRadius_proche() {
        Double result = keyword_parser.extractProximityRadius("restaurant proche");
        assertNotNull(result);
        assertEquals(5.0, result);
    }

    @Test
    @DisplayName("extractProximityRadius - should detect 'loin' as 20km")
    void extractProximityRadius_loin() {
        Double result = keyword_parser.extractProximityRadius("restaurant loin");
        assertNotNull(result);
        assertEquals(20.0, result);
    }

    @Test
    @DisplayName("extractProximityRadius - should return null when no proximity expression")
    void extractProximityRadius_no_proximity() {
        Double result = keyword_parser.extractProximityRadius("restaurant douala");
        assertNull(result);
    }

    // ===== parseWithCity tests =====

    @Test
    @DisplayName("parseWithCity - should parse a complete query with city and category")
    void parseWithCity_complete_query() {
        KeywordParser.ParsedQueryResult result = keyword_parser.parseWithCity("restaurant à douala");
        assertNotNull(result);
        assertEquals("Douala", result.extracted_city);
        assertEquals("Restaurant", result.inferred_category);
        assertFalse(result.is_proximity_search);
    }

    @Test
    @DisplayName("parseWithCity - should detect proximity search")
    void parseWithCity_proximity_search() {
        KeywordParser.ParsedQueryResult result = keyword_parser.parseWithCity("restaurant proche de moi");
        assertNotNull(result);
        assertTrue(result.is_proximity_search);
        assertNotNull(result.proximity_radius);
    }

    // ===== buildQuery tests =====

    @Test
    @DisplayName("buildQuery - should clean query from stopwords and cities")
    void buildQuery_cleans_query() {
        String result = keyword_parser.buildQuery("je veux un restaurant à douala");
        // Should contain "restaurant" but not stopwords or cities
        assertTrue(result.contains("restaurant"));
        assertFalse(result.contains("douala"));
        assertFalse(result.contains("veux"));
    }

    @Test
    @DisplayName("buildQuery - should return empty string for null input")
    void buildQuery_null_input() {
        String result = keyword_parser.buildQuery(null);
        assertEquals("", result);
    }
}
