package com.querylens.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationStateTest {
    @Test
    void pendingRecommendationCanBeAppliedOrDismissed() {
        RecommendationState state = new PendingState();
        assertTrue(state.canTransitionTo("APPLIED"));
        assertTrue(state.canTransitionTo("DISMISSED"));
    }

    @Test
    void appliedRecommendationCannotBeDismissed() {
        RecommendationState state = new AppliedState();
        assertFalse(state.canTransitionTo("DISMISSED"));
    }
}
