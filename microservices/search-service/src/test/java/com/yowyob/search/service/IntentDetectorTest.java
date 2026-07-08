package com.yowyob.search.service;

import com.yowyob.search.domain.logic.IntentDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IntentDetectorTest {

    private IntentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new IntentDetector();
    }

    // ── PRODUCT_SEARCH ────────────────────────────────────────────

    @Test
    void testDetectProductSearchAcheter() {
        assertThat(detector.detect("acheter un téléphone samsung"))
                .isEqualTo(IntentDetector.Intent.PRODUCT_SEARCH);
    }

    @Test
    void testDetectProductSearchCommander() {
        assertThat(detector.detect("commander une pizza en ligne"))
                .isEqualTo(IntentDetector.Intent.PRODUCT_SEARCH);
    }

    @Test
    void testDetectProductSearchDisponible() {
        assertThat(detector.detect("disponible en stock à douala"))
                .isEqualTo(IntentDetector.Intent.PRODUCT_SEARCH);
    }

    // ── PROFILE_SEARCH ────────────────────────────────────────────

    @Test
    void testDetectProfileSearchProfil() {
        assertThat(detector.detect("profil du restaurant le palmier"))
                .isEqualTo(IntentDetector.Intent.PROFILE_SEARCH);
    }

    @Test
    void testDetectProfileSearchEntreprise() {
        assertThat(detector.detect("entreprise de construction yaoundé"))
                .isEqualTo(IntentDetector.Intent.PROFILE_SEARCH);
    }

    @Test
    void testDetectProfileSearchFiche() {
        assertThat(detector.detect("fiche du commerce pharmacie centrale"))
                .isEqualTo(IntentDetector.Intent.PROFILE_SEARCH);
    }

    // ── RECOMMENDATION ────────────────────────────────────────────

    @Test
    void testDetectRecommendationMeilleur() {
        assertThat(detector.detect("meilleur restaurant douala"))
                .isEqualTo(IntentDetector.Intent.RECOMMENDATION);
    }

    @Test
    void testDetectRecommendationOuManger() {
        assertThat(detector.detect("où manger à yaoundé"))
                .isEqualTo(IntentDetector.Intent.RECOMMENDATION);
    }

    @Test
    void testDetectRecommendationJeVeux() {
        assertThat(detector.detect("je veux trouver un hôtel"))
                .isEqualTo(IntentDetector.Intent.RECOMMENDATION);
    }

    @Test
    void testDetectRecommendationFaim() {
        assertThat(detector.detect("j'ai faim"))
                .isEqualTo(IntentDetector.Intent.RECOMMENDATION);
    }

    // ── INFORMATION ───────────────────────────────────────────────

    @Test
    void testDetectInformationHoraire() {
        assertThat(detector.detect("horaire du restaurant le gril"))
                .isEqualTo(IntentDetector.Intent.INFORMATION);
    }

    @Test
    void testDetectInformationPrix() {
        assertThat(detector.detect("prix d'une chambre à l'hôtel Hilton"))
                .isEqualTo(IntentDetector.Intent.INFORMATION);
    }

    @Test
    void testDetectInformationTelephone() {
        assertThat(detector.detect("numéro de téléphone pharmacie centrale"))
                .isEqualTo(IntentDetector.Intent.INFORMATION);
    }

    @Test
    void testDetectInformationAdresse() {
        assertThat(detector.detect("adresse du supermarché"))
                .isEqualTo(IntentDetector.Intent.INFORMATION);
    }

    // ── NAVIGATION ────────────────────────────────────────────────

    @Test
    void testDetectNavigationItineraire() {
        assertThat(detector.detect("itinéraire pour aller à l'aéroport"))
                .isEqualTo(IntentDetector.Intent.NAVIGATION);
    }

    @Test
    void testDetectNavigationCommentAller() {
        assertThat(detector.detect("comment aller à la pharmacie du centre"))
                .isEqualTo(IntentDetector.Intent.NAVIGATION);
    }

    // ── GENERAL ───────────────────────────────────────────────────

    @Test
    void testDetectGeneralQueryNeutrale() {
        assertThat(detector.detect("pizza douala"))
                .isEqualTo(IntentDetector.Intent.GENERAL);
    }

    @Test
    void testDetectGeneralQueryNull() {
        assertThat(detector.detect(null))
                .isEqualTo(IntentDetector.Intent.GENERAL);
    }

    @Test
    void testDetectGeneralQueryVide() {
        assertThat(detector.detect(""))
                .isEqualTo(IntentDetector.Intent.GENERAL);
    }

    @Test
    void testDetectGeneralQueryBlank() {
        assertThat(detector.detect("   "))
                .isEqualTo(IntentDetector.Intent.GENERAL);
    }
}
