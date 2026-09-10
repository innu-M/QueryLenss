package com.querylens.benchmark;

import java.time.Duration;

public record BenchmarkSettings(
        int warmupRuns,
        int measuredRuns,
        Duration queryTimeout,
        int maximumCandidates,
        RankingStrategy rankingStrategy) {

    public BenchmarkSettings {
        if (warmupRuns < 0) {
            throw new IllegalArgumentException("Warm-up runs cannot be negative.");
        }
        if (measuredRuns < 1) {
            throw new IllegalArgumentException("At least one measured run is required.");
        }
        if (queryTimeout == null || queryTimeout.isZero() || queryTimeout.isNegative()) {
            throw new IllegalArgumentException("Query timeout must be positive.");
        }
        if (maximumCandidates < 1) {
            throw new IllegalArgumentException("At least one candidate is required.");
        }
        if (rankingStrategy == null) {
            throw new IllegalArgumentException("A ranking strategy is required.");
        }
    }

    public static BenchmarkSettings defaults() {
        return new BenchmarkSettings(1, 5, Duration.ofSeconds(10), 10, RankingStrategy.MEDIAN);
    }
}
