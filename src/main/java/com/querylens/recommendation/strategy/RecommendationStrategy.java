package com.querylens.recommendation.strategy;

import com.querylens.workspace.model.QueryAnalysis;

import java.util.List;

public interface RecommendationStrategy {
    List<String> recommend(QueryAnalysis analysis);
}



