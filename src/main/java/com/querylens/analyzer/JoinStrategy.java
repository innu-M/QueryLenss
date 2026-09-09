package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.Optional;

public class JoinStrategy implements RecommendationStrategy {
    @Override
    public Optional<Recommendation> recommend(AnalysisResult analysis) {
        if (analysis.joinCount() == 0) return Optional.empty();
        return Optional.of(new Recommendation("Join",
                "Check that each join column is indexed and that the join condition is necessary.", "MEDIUM"));
    }
}
