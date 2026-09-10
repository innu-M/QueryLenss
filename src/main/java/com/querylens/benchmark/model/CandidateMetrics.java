package com.querylens.benchmark.model;

import java.util.Comparator;
import java.util.List;

public record CandidateMetrics(String candidateLabel, List<Long> durationsNanos) {
    public CandidateMetrics {
        if (candidateLabel == null || candidateLabel.isBlank()) {
            throw new IllegalArgumentException("A candidate label is required.");
        }
        durationsNanos = List.copyOf(durationsNanos);
        if (durationsNanos.isEmpty() || durationsNanos.stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Measured durations must be non-negative.");
        }
    }

    public double averageNanos() {
        return durationsNanos.stream().mapToDouble(Long::doubleValue).average().orElse(0);
    }

    public long medianNanos() {
        List<Long> sorted = durationsNanos.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 0 ? (sorted.get(middle - 1) + sorted.get(middle)) / 2 : sorted.get(middle);
    }

    public long p95Nanos() {
        List<Long> sorted = durationsNanos.stream().sorted().toList();
        return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
    }

    public double stabilityScore() {
        double average = averageNanos();
        if (average == 0) {
            return 0;
        }
        double variance = durationsNanos.stream().mapToDouble(value -> Math.pow(value - average, 2)).average().orElse(0);
        return Math.sqrt(variance) / average;
    }

    public static Comparator<CandidateMetrics> comparatorFor(RankingStrategy strategy) {
        return switch (strategy) {
            case MEDIAN -> Comparator.comparingLong(CandidateMetrics::medianNanos);
            case AVERAGE -> Comparator.comparingDouble(CandidateMetrics::averageNanos);
            case P95 -> Comparator.comparingLong(CandidateMetrics::p95Nanos);
            case STABILITY -> Comparator.comparingDouble(CandidateMetrics::stabilityScore).thenComparingLong(CandidateMetrics::medianNanos);
        };
    }
}

