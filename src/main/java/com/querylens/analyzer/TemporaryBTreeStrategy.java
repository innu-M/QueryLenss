package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.Locale;
import java.util.Optional;

public class TemporaryBTreeStrategy implements RecommendationStrategy {
    @Override
    public Optional<Recommendation> recommend(AnalysisResult analysis) {
        boolean temporaryTree = analysis.planSteps().stream()
                .map(step -> step.toUpperCase(Locale.ROOT))
                .anyMatch(step -> step.contains("USE TEMP B-TREE"));
        if (!temporaryTree) return Optional.empty();
        return Optional.of(new Recommendation("Sort / Group",
                "SQLite uses a temporary B-tree for sorting or grouping. Consider an index that matches the ORDER BY or GROUP BY columns.",
                "MEDIUM"));
    }
}
