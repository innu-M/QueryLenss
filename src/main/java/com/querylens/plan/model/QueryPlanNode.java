package com.querylens.plan.model;

import com.querylens.plan.visitor.QueryPlanVisitor;

import java.util.List;

public final class QueryPlanNode implements QueryPlanComponent {
    private final int id;
    private final int parentId;
    private final String detail;
    private final PlanOperationType operationType;
    private final List<QueryPlanNode> children;

    public QueryPlanNode(int id, int parentId, String detail, PlanOperationType operationType,
                         List<QueryPlanNode> children) {
        this.id = id;
        this.parentId = parentId;
        this.detail = detail;
        this.operationType = operationType;
        this.children = List.copyOf(children);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int parentId() {
        return parentId;
    }

    @Override
    public String detail() {
        return detail;
    }

    @Override
    public PlanOperationType operationType() {
        return operationType;
    }

    @Override
    public List<QueryPlanNode> children() {
        return children;
    }

    @Override
    public <T> T accept(QueryPlanVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
