package com.querylens.comparison;

import java.util.List;

public record ComparisonReport(
        String databasePath,
        String originalSql,
        String normalizedQuery,
        List<CandidateComparison> candidates
) {
    public CandidateComparison original() {
        return candidates.stream()
                .filter(candidate -> "Original".equals(candidate.candidate().label()))
                .findFirst()
                .orElseThrow();
    }

    public CandidateComparison winner() {
        return candidates.stream()
                .filter(CandidateComparison::equivalent)
                .filter(candidate -> "VERIFIED".equals(candidate.status()))
                .min(java.util.Comparator.comparingLong(CandidateComparison::medianDurationNs))
                .orElse(original());
    }
}
