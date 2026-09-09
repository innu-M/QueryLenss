package com.querylens.comparison;

import com.querylens.alternative.QueryCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanExplanationServiceTest {
    @Test
    void explainsWhenAnIndexReplacesATableScan() {
        CandidateComparison original = comparison("Original", 100, List.of("SCAN sailors"));
        CandidateComparison alternative = comparison("Indexed", 25,
                List.of("SEARCH sailors USING INDEX idx_sailors_rating (rating=?)"));

        String explanation = new PlanExplanationService().explain(original, alternative);

        assertTrue(explanation.contains("75.0% faster"));
        assertTrue(explanation.contains("indexed search"));
    }

    private CandidateComparison comparison(String label, long duration, List<String> plan) {
        return new CandidateComparison(new QueryCandidate(label, "SELECT 1", "Test candidate"), List.of(duration),
                duration, plan, true, "VERIFIED", 0, Double.NaN, "");
    }
}
