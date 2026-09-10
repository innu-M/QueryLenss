package com.querylens.recommendation.state;

import com.querylens.recommendation.model.RecommendationStatus;

public final class DismissedRecommendationState implements RecommendationState {
    @Override
    public RecommendationStatus status() {
        return RecommendationStatus.DISMISSED;
    }

    @Override
    public RecommendationState apply() {
        throw new IllegalStateException("Dismissed recommendations cannot be applied.");
    }

    @Override
    public RecommendationState dismiss() {
        throw new IllegalStateException("This recommendation is already dismissed.");
    }
}


