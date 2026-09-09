package com.querylens.state;

public class DismissedState implements RecommendationState {
    @Override public String name() { return "DISMISSED"; }
    @Override public boolean canTransitionTo(String nextState) { return "DISMISSED".equals(nextState); }
}
