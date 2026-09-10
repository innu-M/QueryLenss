package com.querylens.history.model;

public record ComparisonHistorySummary(
        long totalSessions,
        long verifiedCandidates,
        double averageImprovementPercent,
        double bestImprovementPercent) {
}
