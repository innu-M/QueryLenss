package com.querylens.plan;

public record QueryPlanRow(int id, int parentId, String detail) {
    public QueryPlanRow {
        if (detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("A query-plan detail is required.");
        }
    }
}
