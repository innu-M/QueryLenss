package com.querylens.recommendation.state;

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



