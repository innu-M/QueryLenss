package com.querylens.recommendation;
import com.querylens.recommendation.state.RecommendationStatus;


public record Recommendation(long id, long analysisId, String message, RecommendationStatus status) {
}


