package com.querylens.plan;

import java.util.List;

public interface QueryPlanComponent {
    int id();

    int parentId();

    String detail();

    PlanOperationType operationType();

    List<QueryPlanNode> children();

    <T> T accept(QueryPlanVisitor<T> visitor);
}
