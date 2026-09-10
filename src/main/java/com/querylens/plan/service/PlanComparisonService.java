package com.querylens.plan.service;

import com.querylens.plan.adapter.QueryPlanProvider;
import com.querylens.plan.builder.QueryPlanTreeBuilder;
import com.querylens.plan.model.PlanInsight;
import com.querylens.plan.model.PlanOperationType;
import com.querylens.plan.model.QueryPlanTree;
import com.querylens.plan.visitor.PlanExplanationVisitor;

import com.querylens.plan.adapter.QueryPlanProvider;
import com.querylens.plan.builder.QueryPlanTreeBuilder;
import com.querylens.plan.model.PlanInsight;
import com.querylens.plan.model.PlanOperationType;
import com.querylens.plan.model.QueryPlanTree;
import com.querylens.plan.visitor.PlanExplanationVisitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PlanComparisonService {
    private final QueryPlanProvider provider;
    private final QueryPlanTreeBuilder builder;
    private final PlanExplanationVisitor explanationVisitor;

    public PlanComparisonService(QueryPlanProvider provider) {
        this(provider, new QueryPlanTreeBuilder(), new PlanExplanationVisitor());
    }

    PlanComparisonService(QueryPlanProvider provider, QueryPlanTreeBuilder builder,
                          PlanExplanationVisitor explanationVisitor) {
        this.provider = provider;
        this.builder = builder;
        this.explanationVisitor = explanationVisitor;
    }

    public PlanComparisonReport compare(Path databasePath, String originalSql, String alternativeSql) {
        QueryPlanTree original = builder.build(provider.inspect(databasePath, originalSql));
        QueryPlanTree alternative = builder.build(provider.inspect(databasePath, alternativeSql));
        List<String> differences = describeDifferences(original, alternative);
        int originalCost = structuralCost(original);
        int alternativeCost = structuralCost(alternative);
        PlanComparisonReport.Verdict verdict = alternativeCost < originalCost
                ? PlanComparisonReport.Verdict.ALTERNATIVE_LOOKS_BETTER
                : alternativeCost > originalCost
                ? PlanComparisonReport.Verdict.ORIGINAL_LOOKS_BETTER
                : PlanComparisonReport.Verdict.STRUCTURALLY_SIMILAR;
        return new PlanComparisonReport(
                original,
                alternative,
                explain(original),
                explain(alternative),
                differences,
                verdict);
    }

    private List<PlanInsight> explain(QueryPlanTree tree) {
        return tree.roots().stream().flatMap(root -> root.accept(explanationVisitor).stream()).toList();
    }

    private List<String> describeDifferences(QueryPlanTree original, QueryPlanTree alternative) {
        List<String> differences = new ArrayList<>();
        compareCount(differences, "full scan", original.count(PlanOperationType.SCAN),
                alternative.count(PlanOperationType.SCAN), true);
        compareCount(differences, "temporary B-tree", original.count(PlanOperationType.TEMPORARY_BTREE),
                alternative.count(PlanOperationType.TEMPORARY_BTREE), true);
        compareCount(differences, "indexed search", original.count(PlanOperationType.SEARCH),
                alternative.count(PlanOperationType.SEARCH), false);
        if (differences.isEmpty()) {
            differences.add("The plans have the same scan, search, and temporary B-tree counts. Benchmark both queries before choosing.");
        }
        return differences;
    }

    private void compareCount(List<String> differences, String operation, int original, int alternative,
                              boolean lowerIsBetter) {
        if (original == alternative) return;
        boolean alternativeImproves = lowerIsBetter ? alternative < original : alternative > original;
        String subject = alternativeImproves ? "Alternative" : "Original";
        differences.add("%s has a better %s count (%d versus %d)."
                .formatted(subject, operation, alternativeImproves ? alternative : original,
                        alternativeImproves ? original : alternative));
    }

    private int structuralCost(QueryPlanTree tree) {
        return tree.count(PlanOperationType.SCAN) * 3
                + tree.count(PlanOperationType.TEMPORARY_BTREE) * 4
                - tree.count(PlanOperationType.SEARCH);
    }
}
