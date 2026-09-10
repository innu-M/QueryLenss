package com.querylens.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QueryPlanTreeBuilder {
    public QueryPlanTree build(List<QueryPlanRow> rows) {
        Map<Integer, QueryPlanNode> nodes = new LinkedHashMap<>();
        for (QueryPlanRow row : rows) {
            if (nodes.containsKey(row.id())) {
                throw new IllegalArgumentException("Duplicate query-plan node ID: " + row.id());
            }
            nodes.put(row.id(), new QueryPlanNode(
                    row.id(), row.parentId(), row.detail(), classify(row.detail())));
        }

        List<QueryPlanNode> roots = new ArrayList<>();
        for (QueryPlanNode node : nodes.values()) {
            QueryPlanNode parent = nodes.get(node.parentId());
            if (parent == null || parent == node) {
                roots.add(node);
            } else {
                parent.addChild(node);
            }
        }
        return new QueryPlanTree(roots);
    }

    PlanOperationType classify(String detail) {
        String normalized = detail.toUpperCase(Locale.ROOT);
        if (normalized.contains("TEMP B-TREE")) return PlanOperationType.TEMPORARY_BTREE;
        if (normalized.startsWith("SCAN") || normalized.contains(" SCAN ")) return PlanOperationType.SCAN;
        if (normalized.startsWith("SEARCH") || normalized.contains(" SEARCH ")) return PlanOperationType.SEARCH;
        if (normalized.contains("COMPOUND") || normalized.contains("UNION") || normalized.contains("CO-ROUTINE")) {
            return PlanOperationType.COMPOUND;
        }
        return PlanOperationType.OTHER;
    }
}
