package com.querylens.observer;

import com.querylens.model.QueryExecutionResult;

public interface QueryExecutionObserver {
    void onQueryExecuted(QueryExecutionResult result);
}
