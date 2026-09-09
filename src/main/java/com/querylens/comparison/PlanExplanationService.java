package com.querylens.comparison;

import java.util.List;
import java.util.Locale;

public class PlanExplanationService {
    public String explain(CandidateComparison original, CandidateComparison candidate) {
        if (!candidate.equivalent()) return "Rejected because its result did not match the original query.";
        if (!"VERIFIED".equals(candidate.status())) return candidate.explanation();
        if (candidate == original) return "Baseline query used for comparison.";

        double improvement = original.medianDurationNs() == 0 ? 0
                : 100.0 * (original.medianDurationNs() - candidate.medianDurationNs()) / original.medianDurationNs();
        StringBuilder explanation = new StringBuilder();
        if (improvement > 0) explanation.append(String.format(Locale.ROOT, "Measured %.1f%% faster. ", improvement));
        else explanation.append(String.format(Locale.ROOT, "Measured %.1f%% slower. ", Math.abs(improvement)));

        String originalPlan = joined(original.planSteps());
        String candidatePlan = joined(candidate.planSteps());
        if (originalPlan.contains("SCAN ") && candidatePlan.contains("SEARCH ")) {
            explanation.append("SQLite replaced a table scan with an indexed search. ");
        }
        if (!originalPlan.contains("USING INDEX") && candidatePlan.contains("USING INDEX")) {
            explanation.append("The alternative uses an index absent from the original plan. ");
        }
        if (originalPlan.contains("USE TEMP B-TREE") && !candidatePlan.contains("USE TEMP B-TREE")) {
            explanation.append("The alternative avoids the original temporary B-tree. ");
        }
        if (originalPlan.equals(candidatePlan)) explanation.append("SQLite produced the same plan, so the timing difference may be noise. ");
        explanation.append(candidate.candidate().rationale());
        return explanation.toString().trim();
    }

    private String joined(List<String> steps) {
        return String.join(" | ", steps).toUpperCase(Locale.ROOT);
    }
}
