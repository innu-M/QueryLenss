package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.Recommendation;

import java.util.Optional;

public interface RecommendationStrategy {
    Optional<Recommendation> recommend(AnalysisResult analysis);
}
