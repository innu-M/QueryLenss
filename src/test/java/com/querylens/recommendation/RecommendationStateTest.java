package com.querylens.recommendation;

import com.querylens.recommendation.state.AppliedRecommendationState;
import com.querylens.recommendation.state.DismissedRecommendationState;
import com.querylens.recommendation.state.PendingRecommendationState;
import com.querylens.recommendation.state.RecommendationState;
import com.querylens.recommendation.model.RecommendationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecommendationStateTest {
    @Test
    void pendingRecommendationCanBeAppliedOrDismissed() {
        RecommendationState pending = new PendingRecommendationState();

        assertEquals(RecommendationStatus.APPLIED, pending.apply().status());
        assertEquals(RecommendationStatus.DISMISSED, pending.dismiss().status());
    }

    @Test
    void completedStatesCannotMoveBackwards() {
        assertThrows(IllegalStateException.class, () -> new AppliedRecommendationState().dismiss());
        assertThrows(IllegalStateException.class, () -> new DismissedRecommendationState().apply());
    }
}

