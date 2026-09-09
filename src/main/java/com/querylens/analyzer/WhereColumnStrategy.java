package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.Locale;
import java.util.Optional;

public class WhereColumnStrategy implements RecommendationStrategy {
    @Override
    public Optional<Recommendation> recommend(AnalysisResult analysis) {
        if (analysis.whereColumns().isEmpty() || analysis.tables().isEmpty()) return Optional.empty();
        String column = analysis.whereColumns().getFirst();
        String table = analysis.tables().getFirst();
        if (analysis.indexedWhereColumns().stream().anyMatch(indexed -> indexed.equalsIgnoreCase(column))) {
            return Optional.empty();
        }
        boolean fullScan = analysis.planSteps().stream()
                .map(step -> step.toUpperCase(Locale.ROOT))
                .anyMatch(step -> step.startsWith("SCAN ") && !step.contains("USING "));
        if (!fullScan) return Optional.empty();
        return Optional.of(new Recommendation("Index",
                "SQLite selected a full table scan. For frequent queries, consider: CREATE INDEX idx_" + table + "_" + column
                        + " ON " + table + "(" + column + ");", "HIGH"));
    }
}
