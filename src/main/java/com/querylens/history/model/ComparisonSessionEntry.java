package com.querylens.history.model;

public record ComparisonSessionEntry(
        long id,
        String databasePath,
        String originalSql,
        String rankingStrategy,
        String createdAt,
        int candidateCount,
        String winnerLabel,
        long originalMedianNs,
        long winnerMedianNs) {

    public double improvementPercent() {
        if (originalMedianNs <= 0 || winnerMedianNs <= 0) return 0;
        return 100.0 * (originalMedianNs - winnerMedianNs) / originalMedianNs;
    }
}
