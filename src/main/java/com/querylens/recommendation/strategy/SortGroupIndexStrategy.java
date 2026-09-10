package com.querylens.recommendation.strategy;

import com.querylens.workspace.model.QueryAnalysis;

import java.util.List;

public final class SortGroupIndexStrategy implements RecommendationStrategy {
    @Override
    public List<String> recommend(QueryAnalysis analysis) {
        return List.of("Consider an index that supports this query's ORDER BY or GROUP BY clause.");
    }
}



