package com.yowyob.search.service;
import com.yowyob.search.domain.logic.KeywordParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProximityExtractionTest {

    private KeywordParser parser;

    @BeforeEach
    void setUp() {
        parser = new KeywordParser();
    }

    @Test
    void testExtractProximityRadiusPresDeChezmoi() {
        // Tester avec version sans accent
        Double radius = parser.extractProximityRadius("restaurants pres de chez moi");
        assertThat(radius).isNotNull().isEqualTo(5.0);
    }

    @Test
    void testExtractProximityRadiusProche() {
        Double radius = parser.extractProximityRadius("hôtels proche de moi");
        assertThat(radius).isNotNull().isEqualTo(5.0);
    }

    @Test
    void testExtractProximityRadiusLoin() {
        Double radius = parser.extractProximityRadius("restaurants loin");
        assertThat(radius).isNotNull().isEqualTo(20.0);
    }

    @Test
    void testExtractProximityRadiusAutour() {
        Double radius = parser.extractProximityRadius("agences autour de moi");
        assertThat(radius).isNotNull().isEqualTo(5.0);
    }

    @Test
    void testExtractProximityRadiusNone() {
        Double radius = parser.extractProximityRadius("restaurants douala");
        assertThat(radius).isNull();
    }

    @Test
    void testParseWithProximity() {
        // Tester avec version sans accent
        KeywordParser.ParsedQueryResult result = parser.parseWithCity("restaurants pres de chez moi");
        assertThat(result.query).isEqualTo("restaurant");
        assertThat(result.isProximitySearch).isTrue();
        assertThat(result.proximityRadius).isEqualTo(5.0);
    }

    @Test
    void testParseWithoutProximity() {
        KeywordParser.ParsedQueryResult result = parser.parseWithCity("restaurants douala");
        assertThat(result.query).isEqualTo("restaurant");
        assertThat(result.isProximitySearch).isFalse();
        assertThat(result.proximityRadius).isNull();
    }
}
