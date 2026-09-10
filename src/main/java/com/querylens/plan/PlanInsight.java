package com.querylens.plan;

public record PlanInsight(int nodeId, Severity severity, String message) {
    public enum Severity {
        INFORMATION,
        WARNING,
        EXPENSIVE
    }
}
