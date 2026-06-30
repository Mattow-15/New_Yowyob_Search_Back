package com.yowyob.search.service;
import com.yowyob.search.domain.logic.KeywordParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class KeywordParserTest {

    private KeywordParser parser;

    @BeforeEach
    void setUp() {
        parser = new KeywordParser();
    }

    @Test
    void testExtractKeywordsSimplePhrase() {
        List<String> keywords = parser.extractKeywords("je veux les agences de voyage à yaoundé");
        assertThat(keywords).containsExactly("agence", "voyage");
    }

    @Test
    void testBuildQueryJoinsWithSpace() {
        String query = parser.buildQuery("je veux les agences de voyage à yaoundé");
        assertThat(query).isEqualTo("agence voyage");
    }

    @Test
    void testExtractKeywordsPunctuation() {
        List<String> keywords = parser.extractKeywords("hôtels, agences de voyage!");
        assertThat(keywords).contains("hôtel", "agence", "voyage");
    }

    @Test
    void testBuildQueryWithEmptyInput() {
        String query = parser.buildQuery("");
        assertThat(query).isEqualTo("");
    }

    @Test
    void testBuildQueryWithOnlyStopwords() {
        // Quand tous les mots sont des stopwords, le parseur retourne la phrase originale
        String query = parser.buildQuery("de la les un");
        assertThat(query).isEqualTo("de la les un");
    }

    @Test
    void testExtractKeywordsSingularization() {
        List<String> keywords = parser.extractKeywords("agences voyages");
        assertThat(keywords).containsExactly("agence", "voyage");
    }

    @Test
    void testExtractCityDouala() {
        String city = parser.extractCity("je veux les agences de voyage à douala");
        assertThat(city).isEqualTo("Douala");
    }

    @Test
    void testExtractCityYaoundé() {
        String city = parser.extractCity("hôtels yaoundé");
        assertThat(city).isEqualTo("Yaoundé");
    }

    @Test
    void testExtractCityBuea() {
        String city = parser.extractCity("restaurants à buea");
        assertThat(city).isEqualTo("Buea");
    }

    @Test
    void testExtractCityNone() {
        String city = parser.extractCity("je veux un hôtel");
        assertThat(city).isNull();
    }

    @Test
    void testParseWithCityExtraction() {
        KeywordParser.ParsedQueryResult result = parser.parseWithCity("je veux les agences de voyage à douala");
        assertThat(result.query).isEqualTo("agence voyage");
        assertThat(result.extractedCity).isEqualTo("Douala");
    }

    @Test
    void testParseWithoutCity() {
        KeywordParser.ParsedQueryResult result = parser.parseWithCity("je veux un hôtel");
        assertThat(result.query).isEqualTo("hôtel");
        assertThat(result.extractedCity).isNull();
    }
}

