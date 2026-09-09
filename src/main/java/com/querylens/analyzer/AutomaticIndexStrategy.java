package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.Locale;
import java.util.Optional;

public class AutomaticIndexStrategy implements RecommendationStrategy {
    @Override
    public Optional<Recommendation> recommend(AnalysisResult analysis) {
        boolean automaticIndex = analysis.planSteps().stream()
                .map(step -> step.toUpperCase(Locale.ROOT))
                .anyMatch(step -> step.contains("AUTOMATIC INDEX"));
        if (!automaticIndex) return Optional.empty();
        return Optional.of(new Recommendation("Index",
                "SQLite created an automatic index for this plan. Consider creating a permanent index for this repeated workload.",
                "HIGH"));
    }
}
