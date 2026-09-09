package com.querylens.state;

public class AppliedState implements RecommendationState {
    @Override public String name() { return "APPLIED"; }
    @Override public boolean canTransitionTo(String nextState) { return "APPLIED".equals(nextState); }
}
