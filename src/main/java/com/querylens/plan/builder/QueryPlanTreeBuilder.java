package com.querylens.plan.builder;

import com.querylens.plan.model.PlanOperationType;
import com.querylens.plan.model.QueryPlanNode;
import com.querylens.plan.model.QueryPlanRow;
import com.querylens.plan.model.QueryPlanTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QueryPlanTreeBuilder {
    public QueryPlanTree build(List<QueryPlanRow> rows) {
        Map<Integer, QueryPlanRow> rowsById = new LinkedHashMap<>();
        for (QueryPlanRow row : rows) {
            if (rowsById.putIfAbsent(row.id(), row) != null) {
                throw new IllegalArgumentException("Duplicate query-plan node ID: " + row.id());
            }
        }

        Map<Integer, List<QueryPlanRow>> childrenByParent = new LinkedHashMap<>();
        List<QueryPlanRow> rootRows = new ArrayList<>();
        for (QueryPlanRow row : rowsById.values()) {
            if (row.parentId() == row.id() || !rowsById.containsKey(row.parentId())) {
                rootRows.add(row);
            } else {
                childrenByParent.computeIfAbsent(row.parentId(), ignored -> new ArrayList<>()).add(row);
            }
        }

        Set<Integer> builtNodeIds = new HashSet<>();
        List<QueryPlanNode> roots = rootRows.stream()
                .map(row -> buildNode(row, childrenByParent, new HashSet<>(), builtNodeIds))
                .toList();
        if (builtNodeIds.size() != rowsById.size()) {
            throw new IllegalArgumentException("Query-plan rows contain a parent cycle.");
        }
        return new QueryPlanTree(roots);
    }

    private QueryPlanNode buildNode(QueryPlanRow row,
                                    Map<Integer, List<QueryPlanRow>> childrenByParent,
                                    Set<Integer> ancestors,
                                    Set<Integer> builtNodeIds) {
        if (!ancestors.add(row.id())) {
            throw new IllegalArgumentException("Query-plan rows contain a cycle at node " + row.id() + ".");
        }
        List<QueryPlanNode> children = childrenByParent.getOrDefault(row.id(), List.of()).stream()
                .map(child -> buildNode(child, childrenByParent, new HashSet<>(ancestors), builtNodeIds))
                .toList();
        builtNodeIds.add(row.id());
        return new QueryPlanNode(row.id(), row.parentId(), row.detail(), classify(row.detail()), children);
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
