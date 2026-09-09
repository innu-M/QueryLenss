package com.querylens.state;

public class PendingState implements RecommendationState {
    @Override public String name() { return "PENDING"; }
    @Override public boolean canTransitionTo(String nextState) {
        return "APPLIED".equals(nextState) || "DISMISSED".equals(nextState);
    }
}
