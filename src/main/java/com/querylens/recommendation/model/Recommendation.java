package com.querylens.recommendation.model;


public record Recommendation(long id, long analysisId, String message, RecommendationStatus status) {
}

