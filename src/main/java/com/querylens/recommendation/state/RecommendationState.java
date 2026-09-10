package com.querylens.recommendation.state;

public interface RecommendationState {
    RecommendationStatus status();

    RecommendationState apply();

    RecommendationState dismiss();
}



