package com.nikhil.analytics;

import com.nikhil.analytics.scoring.News2Calculator;
import com.nikhil.analytics.scoring.News2Result;
import com.nikhil.analytics.scoring.RiskLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class News2CalculatorTest {

    @Test
    void healthyAdultScoresZero() {
        News2Result r = News2Calculator.score(16.0, 98.0, 125.0, 72.0, 36.8);
        assertThat(r.total()).isZero();
        assertThat(r.level()).isEqualTo(RiskLevel.LOW);
        assertThat(r.requiresAlert()).isFalse();
        assertThat(r.missing()).isEmpty();
    }

    @Test
    void septicPictureIsHigh() {
        // RR 26 (3) + SpO2 92 (2) + SBP 95 (2) + HR 125 (2) + T 38.6 (1) = 10
        News2Result r = News2Calculator.score(26.0, 92.0, 95.0, 125.0, 38.6);
        assertThat(r.total()).isEqualTo(10);
        assertThat(r.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.reasons()).hasSize(5);
    }

    @Test
    void reasonsShowTheExactValueThatWasScored() {
        // 91.4 is above 91, so it scores +2; the label must not round it to "91"
        News2Result r = News2Calculator.score(16.0, 91.4, 125.0, 72.0, 39.25);
        assertThat(r.reasons()).contains("SpO2 91.4% (+2)", "Temperature 39.25 C (+2)");
        assertThat(News2Calculator.score(26.0, null, null, null, null).reasons()).containsExactly("Respiratory rate 26/min (+3)");
    }

    @Test
    void medium() {
        // RR 22 (2) + HR 115 (2) + T 38.5 (1) = 5
        assertThat(News2Calculator.score(22.0, 97.0, 130.0, 115.0, 38.5).level()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void singleRedParameterIsLowMedium() {
        // SBP 88 alone scores 3, total 3
        News2Result r = News2Calculator.score(16.0, 98.0, 88.0, 80.0, 37.0);
        assertThat(r.total()).isEqualTo(3);
        assertThat(r.level()).isEqualTo(RiskLevel.LOW_MEDIUM);
        assertThat(r.requiresAlert()).isTrue();
    }

    @Test
    void missingParametersAreReportedAndScoreZero() {
        News2Result r = News2Calculator.score(null, null, null, 135.0, null);
        assertThat(r.total()).isEqualTo(3);
        assertThat(r.missing()).containsExactly("respiratoryRate", "spo2", "systolicBp", "temperatureC");
        assertThat(r.components()).containsOnlyKeys("heartRate");
    }

    // Boundary values straight from the NEWS2 chart.
    @ParameterizedTest
    @CsvSource({
            "8,3", "9,1", "11,1", "12,0", "20,0", "21,2", "24,2", "25,3"
    })
    void respirationBands(double rr, int expected) {
        assertThat(News2Calculator.score(rr, null, null, null, null).total()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"91,3", "92,2", "93,2", "94,1", "95,1", "96,0", "100,0"})
    void spo2Bands(double spo2, int expected) {
        assertThat(News2Calculator.score(null, spo2, null, null, null).total()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"90,3", "91,2", "100,2", "101,1", "110,1", "111,0", "219,0", "220,3"})
    void systolicBands(double sbp, int expected) {
        assertThat(News2Calculator.score(null, null, sbp, null, null).total()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"40,3", "41,1", "50,1", "51,0", "90,0", "91,1", "110,1", "111,2", "130,2", "131,3"})
    void pulseBands(double hr, int expected) {
        assertThat(News2Calculator.score(null, null, null, hr, null).total()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"35.0,3", "35.1,1", "36.0,1", "36.1,0", "38.0,0", "38.1,1", "39.0,1", "39.1,2"})
    void temperatureBands(double t, int expected) {
        assertThat(News2Calculator.score(null, null, null, null, t).total()).isEqualTo(expected);
    }
}
