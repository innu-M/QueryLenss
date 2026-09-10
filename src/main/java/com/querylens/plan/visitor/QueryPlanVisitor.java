package com.querylens.plan.visitor;

import com.querylens.plan.model.QueryPlanNode;

import com.querylens.plan.model.QueryPlanNode;

@FunctionalInterface
public interface QueryPlanVisitor<T> {
    T visit(QueryPlanNode node);
}
