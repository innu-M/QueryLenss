package com.querylens.state;

public interface RecommendationState {
    String name();
    boolean canTransitionTo(String nextState);
}
