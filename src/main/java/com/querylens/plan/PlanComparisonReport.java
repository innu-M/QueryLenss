package com.querylens.plan;

import java.util.List;

public record PlanComparisonReport(
        QueryPlanTree originalPlan,
        QueryPlanTree alternativePlan,
        List<PlanInsight> originalInsights,
        List<PlanInsight> alternativeInsights,
        List<String> differences,
        Verdict verdict) {

    public PlanComparisonReport {
        originalInsights = List.copyOf(originalInsights);
        alternativeInsights = List.copyOf(alternativeInsights);
        differences = List.copyOf(differences);
    }

    public enum Verdict {
        ALTERNATIVE_LOOKS_BETTER,
        ORIGINAL_LOOKS_BETTER,
        STRUCTURALLY_SIMILAR
    }
}
