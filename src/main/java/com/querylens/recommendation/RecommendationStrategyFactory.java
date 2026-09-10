package com.querylens.recommendation;

import com.querylens.workspace.QueryAnalysis;

import java.util.ArrayList;
import java.util.List;

public final class RecommendationStrategyFactory {
    public List<RecommendationStrategy> forAnalysis(QueryAnalysis analysis) {
        List<RecommendationStrategy> strategies = new ArrayList<>();
        if (analysis.selectsAllColumns()) {
            strategies.add(new SelectStarRecommendationStrategy());
        }
        if (!analysis.filteredColumns().isEmpty()) {
            strategies.add(new FilteredColumnIndexStrategy());
        }
        if (analysis.hasOrderingOrGrouping()) {
            strategies.add(new SortGroupIndexStrategy());
        }
        if (analysis.joinCount() > 0) {
            strategies.add(new JoinIndexRecommendationStrategy());
        }
        return List.copyOf(strategies);
    }
}
