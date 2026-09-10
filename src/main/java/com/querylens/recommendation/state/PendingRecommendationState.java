package com.querylens.recommendation.state;

public final class PendingRecommendationState implements RecommendationState {
    @Override
    public RecommendationStatus status() {
        return RecommendationStatus.PENDING;
    }

    @Override
    public RecommendationState apply() {
        return new AppliedRecommendationState();
    }

    @Override
    public RecommendationState dismiss() {
        return new DismissedRecommendationState();
    }
}



