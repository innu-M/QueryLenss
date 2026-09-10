package com.querylens.history;

import com.querylens.benchmark.CandidateMetrics;

import java.util.List;

public record ComparisonCandidateDraft(
        String label,
        String sql,
        List<Long> durationSamplesNs,
        boolean equivalent,
        String status,
        int rank,
        String explanation,
        String planText) {

    public ComparisonCandidateDraft {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("A candidate label is required.");
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("Candidate SQL is required.");
        durationSamplesNs = List.copyOf(durationSamplesNs);
        if (durationSamplesNs.stream().anyMatch(value -> value == null || value < 0)) {
            throw new IllegalArgumentException("Benchmark samples must be non-negative.");
        }
        if (status == null || status.isBlank()) throw new IllegalArgumentException("A candidate status is required.");
        if (rank < 0) throw new IllegalArgumentException("Candidate rank cannot be negative.");
        explanation = explanation == null ? "" : explanation;
        planText = planText == null ? "" : planText;
    }

    public long medianNs() {
        if (durationSamplesNs.isEmpty()) return 0;
        return new CandidateMetrics(label, durationSamplesNs).medianNanos();
    }

    public long p95Ns() {
        if (durationSamplesNs.isEmpty()) return 0;
        return new CandidateMetrics(label, durationSamplesNs).p95Nanos();
    }
}
