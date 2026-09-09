package com.querylens.alternative;

import java.util.List;

public class QueryRewriteStrategyFactory {
    public List<QueryRewriteStrategy> createStrategies() {
        return List.of(new IndexPlanStrategy());
    }
}
