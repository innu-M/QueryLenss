package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.Optional;

public class SelectStarStrategy implements RecommendationStrategy {
    @Override
    public Optional<Recommendation> recommend(AnalysisResult analysis) {
        if (!analysis.usesSelectStar()) return Optional.empty();
        return Optional.of(new Recommendation("Query rewrite",
                "Replace SELECT * with only the columns the screen needs.", "MEDIUM"));
    }
}
