package com.querylens.alternative;

import com.querylens.alternative.model.GeneratedQueryCandidate;
import com.querylens.alternative.service.CandidateRanker;
import com.querylens.alternative.service.CandidateRanker.CandidateMeasurement;
import com.querylens.benchmark.model.BenchmarkSettings;
import com.querylens.benchmark.model.RankingStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateRankerTest {
    private final CandidateRanker ranker = new CandidateRanker();
    private final BenchmarkSettings settings =
            new BenchmarkSettings(0, 3, Duration.ofSeconds(1), 3, RankingStrategy.MEDIAN);

    @Test
    void ranksEquivalentCandidatesAndCalculatesLocalPercentile() {
        List<CandidateMeasurement> candidates = List.of(
                measurement("Original", "SELECT original", List.of(90L, 100L, 110L), true),
                measurement("Indexed", "SELECT indexed", List.of(40L, 50L, 60L), true),
                measurement("Rejected", "SELECT rejected", List.of(), false));

        var ranked = ranker.rank(candidates, settings);

        assertEquals(1, ranked.get(1).rank());
        assertEquals(100.0, ranked.get(1).beatsPercent());
        assertEquals(0, ranked.get(2).rank());
        assertTrue(ranked.get(1).explanation().contains("50.0% faster"));
    }

    @Test
    void measurementSamplesAreImmutable() {
        CandidateMeasurement measurement = measurement(
                "Original", "SELECT 1", List.of(10L), true);

        assertThrows(UnsupportedOperationException.class, () -> measurement.samples().add(20L));
    }

    private CandidateMeasurement measurement(String label, String sql, List<Long> samples, boolean equivalent) {
        return new CandidateMeasurement(
                new GeneratedQueryCandidate(label, sql, "Reason."),
                samples,
                equivalent,
                equivalent ? "VERIFIED" : "REJECTED_NOT_EQUIVALENT",
                "plan");
    }
}
