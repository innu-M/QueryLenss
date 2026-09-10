package com.querylens.recommendation.state;

import com.querylens.recommendation.model.RecommendationStatus;

public interface RecommendationState {
    RecommendationStatus status();

    RecommendationState apply();

    RecommendationState dismiss();
}


