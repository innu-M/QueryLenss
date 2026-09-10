package com.querylens.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlanExplanationVisitor implements QueryPlanVisitor<List<PlanInsight>> {
    @Override
    public List<PlanInsight> visit(QueryPlanNode node) {
        List<PlanInsight> insights = new ArrayList<>();
        insights.add(explain(node));
        for (QueryPlanNode child : node.children()) {
            insights.addAll(child.accept(this));
        }
        return List.copyOf(insights);
    }

    private PlanInsight explain(QueryPlanNode node) {
        String detail = node.detail();
        String normalized = detail.toUpperCase(Locale.ROOT);
        return switch (node.operationType()) {
            case SCAN -> new PlanInsight(node.id(), PlanInsight.Severity.WARNING,
                    "Full scan: SQLite may inspect every row for this operation. " + detail);
            case SEARCH -> {
                boolean automatic = normalized.contains("AUTOMATIC");
                yield new PlanInsight(node.id(),
                        automatic ? PlanInsight.Severity.WARNING : PlanInsight.Severity.INFORMATION,
                        (automatic ? "Automatic index search: consider a permanent index. " :
                                "Indexed search: SQLite can narrow the rows it reads. ") + detail);
            }
            case TEMPORARY_BTREE -> new PlanInsight(node.id(), PlanInsight.Severity.EXPENSIVE,
                    "Temporary B-tree: sorting, grouping, or duplicate removal needs extra work. " + detail);
            case COMPOUND -> new PlanInsight(node.id(), PlanInsight.Severity.INFORMATION,
                    "Compound operation: SQLite coordinates multiple plan branches. " + detail);
            case OTHER -> new PlanInsight(node.id(), PlanInsight.Severity.INFORMATION,
                    "Plan operation: " + detail);
        };
    }
}
