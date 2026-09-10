package com.querylens.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QueryPlanNode implements QueryPlanComponent {
    private final int id;
    private final int parentId;
    private final String detail;
    private final PlanOperationType operationType;
    private final List<QueryPlanNode> children = new ArrayList<>();

    QueryPlanNode(int id, int parentId, String detail, PlanOperationType operationType) {
        this.id = id;
        this.parentId = parentId;
        this.detail = detail;
        this.operationType = operationType;
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
        return Collections.unmodifiableList(children);
    }

    @Override
    public <T> T accept(QueryPlanVisitor<T> visitor) {
        return visitor.visit(this);
    }

    void addChild(QueryPlanNode child) {
        children.add(child);
    }
}
