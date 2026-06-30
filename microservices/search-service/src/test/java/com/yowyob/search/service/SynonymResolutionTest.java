package com.yowyob.search.service;
import com.yowyob.search.domain.logic.KeywordParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SynonymResolutionTest {

    private KeywordParser parser;

    @BeforeEach
    void setUp() {
        parser = new KeywordParser();
    }

    @Test
    void testResolveSynonymResto() {
        // "resto" devrait être résolu en "restaurant"
        String query = parser.buildQuery("resto");
        assertThat(query).isEqualTo("restaurant");
    }

    @Test
    void testResolveSynonymCafe() {
        // "cafe" devrait être résolu en "restaurant"
        String query = parser.buildQuery("cafe");
        assertThat(query).isEqualTo("restaurant");
    }

    @Test
    void testResolveSynonymTour() {
        // "tour" devrait être résolu en "agence"
        String query = parser.buildQuery("tour");
        assertThat(query).isEqualTo("agence");
    }

    @Test
    void testResolveSynonymLodge() {
        // "lodge" devrait être résolu en "hotel"
        String query = parser.buildQuery("lodge");
        assertThat(query).isEqualTo("hotel");
    }

    @Test
    void testResolveSynonymAuberge() {
        // "auberge" devrait être résolu en "hotel"
        String query = parser.buildQuery("auberge");
        assertThat(query).isEqualTo("hotel");
    }

    @Test
    void testMultipleSynonyms() {
        // Plusieurs synonymes dans une requête
        String query = parser.buildQuery("resto et cafe");
        assertThat(query).contains("restaurant");
    }

    @Test
    void testSynonymWithStopwords() {
        // Les stopwords sont supprimés, les synonymes appliqués
        String query = parser.buildQuery("je veux un resto pres de chez moi");
        assertThat(query).isEqualTo("restaurant");
    }

    @Test
    void testNoSynonymMatch() {
        // Mot sans synonyme reste inchangé
        String query = parser.buildQuery("pizza");
        assertThat(query).isEqualTo("pizza");
    }

    @Test
    void testSynonymCaseInsensitive() {
        // Les synonymes fonctionnent quelle que soit la casse
        String query = parser.buildQuery("RESTO");
        assertThat(query).isEqualTo("restaurant");
    }
}
