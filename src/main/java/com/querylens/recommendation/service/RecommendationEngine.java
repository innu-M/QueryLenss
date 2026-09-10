package com.querylens.recommendation.service;
import com.querylens.recommendation.strategy.RecommendationStrategyFactory;

import com.querylens.workspace.model.QueryAnalysis;

import java.util.List;

public final class RecommendationEngine {
    private final RecommendationStrategyFactory factory;

    public RecommendationEngine(RecommendationStrategyFactory factory) {
        this.factory = factory;
    }

    public List<String> generate(QueryAnalysis analysis) {
        return factory.forAnalysis(analysis).stream()
                .flatMap(strategy -> strategy.recommend(analysis).stream())
                .toList();
    }
}


