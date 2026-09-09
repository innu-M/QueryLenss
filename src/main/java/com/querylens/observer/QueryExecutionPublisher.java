package com.querylens.observer;

import com.querylens.model.QueryExecutionResult;

import java.util.ArrayList;
import java.util.List;

public class QueryExecutionPublisher {
    private final List<QueryExecutionObserver> observers = new ArrayList<>();

    public void subscribe(QueryExecutionObserver observer) {
        observers.add(observer);
    }

    public void publish(QueryExecutionResult result) {
        observers.forEach(observer -> observer.onQueryExecuted(result));
    }
}
