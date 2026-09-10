package com.querylens.plan.model;

public record PlanInsight(int nodeId, Severity severity, String message) {
    public enum Severity {
        INFORMATION,
        WARNING,
        EXPENSIVE
    }
}
