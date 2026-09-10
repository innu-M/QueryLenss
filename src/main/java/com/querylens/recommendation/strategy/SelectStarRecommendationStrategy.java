package com.querylens.recommendation.strategy;

import com.querylens.workspace.QueryAnalysis;

import java.util.List;

public final class SelectStarRecommendationStrategy implements RecommendationStrategy {
    @Override
    public List<String> recommend(QueryAnalysis analysis) {
        return List.of("Select only the columns you need instead of using SELECT *.");
    }
}



