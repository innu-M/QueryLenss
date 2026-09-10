package com.querylens.recommendation.strategy;

import com.querylens.workspace.model.QueryAnalysis;

import java.util.List;

public final class FilteredColumnIndexStrategy implements RecommendationStrategy {
    @Override
    public List<String> recommend(QueryAnalysis analysis) {
        return analysis.filteredColumns().stream()
                .map(column -> "Consider an index on filtered column '" + column + "'.")
                .toList();
    }
}



