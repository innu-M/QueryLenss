package com.querylens.plan.model;

import com.querylens.plan.visitor.QueryPlanVisitor;

import java.util.List;

public interface QueryPlanComponent {
    int id();

    int parentId();

    String detail();

    PlanOperationType operationType();

    List<? extends QueryPlanComponent> children();

    <T> T accept(QueryPlanVisitor<T> visitor);
}
