package com.querylens.state;

public class RecommendationStateFactory {
    public RecommendationState from(String value) {
        return switch (value) {
            case "APPLIED" -> new AppliedState();
            case "DISMISSED" -> new DismissedState();
            default -> new PendingState();
        };
    }
}
