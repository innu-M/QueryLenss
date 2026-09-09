package com.querylens.comparison;

import com.querylens.alternative.QueryCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MedianTimeRankingStrategyTest {
    @Test
    void ranksOnlyVerifiedEquivalentCandidates() {
        CandidateComparison slow = candidate("Slow", 30, true, "VERIFIED");
        CandidateComparison fast = candidate("Fast", 10, true, "VERIFIED");
        CandidateComparison rejected = candidate("Rejected", 1, false, "NOT_EQUIVALENT");

        var ranked = new MedianTimeRankingStrategy().rank(List.of(slow, fast, rejected));

        assertEquals(2, ranked.get(0).rank());
        assertEquals(1, ranked.get(1).rank());
        assertEquals(0, ranked.get(2).rank());
    }

    private CandidateComparison candidate(String label, long duration, boolean equivalent, String status) {
        return new CandidateComparison(new QueryCandidate(label, "SELECT 1", "Test"), List.of(duration), duration,
                List.of("SCAN test"), equivalent, status, 0, Double.NaN, "");
    }
}
