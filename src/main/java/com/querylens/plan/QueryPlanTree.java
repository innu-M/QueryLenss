package com.querylens.plan;

import java.util.List;

public record QueryPlanTree(List<QueryPlanNode> roots) {
    public QueryPlanTree {
        roots = List.copyOf(roots);
    }

    public int count(PlanOperationType type) {
        return roots.stream().mapToInt(root -> count(root, type)).sum();
    }

    private int count(QueryPlanNode node, PlanOperationType type) {
        int ownCount = node.operationType() == type ? 1 : 0;
        return ownCount + node.children().stream().mapToInt(child -> count(child, type)).sum();
    }
}
