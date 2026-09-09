package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.List;

public class RecommendationEngine {
    private final List<RecommendationStrategy> strategies = new RecommendationStrategyFactory().createStrategies();

    public List<Recommendation> recommend(AnalysisResult analysis) {
        return strategies.stream()
                .map(strategy -> strategy.recommend(analysis))
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
