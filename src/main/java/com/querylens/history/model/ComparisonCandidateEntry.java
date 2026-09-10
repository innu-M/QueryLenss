package com.querylens.history.model;

public record ComparisonCandidateEntry(
        long id,
        long comparisonId,
        String label,
        String sql,
        long medianNs,
        long p95Ns,
        boolean equivalent,
        String status,
        int rank,
        String explanation,
        String planText,
        int sampleCount) {
}
