package com.querylens.plan;

@FunctionalInterface
public interface QueryPlanVisitor<T> {
    T visit(QueryPlanNode node);
}
