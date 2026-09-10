package com.querylens.recommendation.state;

import com.querylens.recommendation.model.RecommendationStatus;

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


