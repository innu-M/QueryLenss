package com.querylens.history;

public record ComparisonHistorySummary(
        long totalSessions,
        long verifiedCandidates,
        double averageImprovementPercent,
        double bestImprovementPercent) {
}
