package com.querylens.recommendation.strategy;

import com.querylens.workspace.model.QueryAnalysis;

import java.util.List;

public final class JoinIndexRecommendationStrategy implements RecommendationStrategy {
    @Override
    public List<String> recommend(QueryAnalysis analysis) {
        return List.of("Check that both sides of the join use indexed columns.");
    }
}



