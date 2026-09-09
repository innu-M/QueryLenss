package com.querylens.analyzer;

import java.util.List;

public class RecommendationStrategyFactory {
    public List<RecommendationStrategy> createStrategies() {
        return List.of(
                new SelectStarStrategy(), new JoinStrategy(), new WhereColumnStrategy(),
                new AutomaticIndexStrategy(), new TemporaryBTreeStrategy()
        );
    }
}
