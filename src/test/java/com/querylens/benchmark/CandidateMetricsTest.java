package com.querylens.benchmark;

import com.querylens.benchmark.model.CandidateMetrics;
import com.querylens.benchmark.model.RankingStrategy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CandidateMetricsTest {
    @Test
    void calculatesMedianAndP95FromMeasuredRuns() {
        CandidateMetrics metrics = new CandidateMetrics("indexed", List.of(30L, 10L, 20L, 40L, 50L));

        assertEquals(30, metrics.medianNanos());
        assertEquals(50, metrics.p95Nanos());
    }

    @Test
    void medianStrategyPrefersTheFasterCandidate() {
        CandidateMetrics slow = new CandidateMetrics("scan", List.of(90L, 100L, 110L));
        CandidateMetrics fast = new CandidateMetrics("index", List.of(40L, 50L, 60L));

        assertEquals(fast, CandidateMetrics.comparatorFor(RankingStrategy.MEDIAN).compare(fast, slow) < 0 ? fast : slow);
    }
}
