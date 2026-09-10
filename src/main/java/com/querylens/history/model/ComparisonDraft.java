package com.querylens.history.model;

import com.querylens.benchmark.model.RankingStrategy;

import java.util.List;

public record ComparisonDraft(
        String databasePath,
        String originalSql,
        RankingStrategy rankingStrategy,
        List<ComparisonCandidateDraft> candidates) {

    public ComparisonDraft {
        if (databasePath == null || databasePath.isBlank()) throw new IllegalArgumentException("A database path is required.");
        if (originalSql == null || originalSql.isBlank()) throw new IllegalArgumentException("Original SQL is required.");
        if (rankingStrategy == null) throw new IllegalArgumentException("A ranking strategy is required.");
        candidates = List.copyOf(candidates);
        if (candidates.isEmpty()) throw new IllegalArgumentException("At least one candidate is required.");
    }
}
