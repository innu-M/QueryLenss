package com.querylens.observer;

import com.querylens.alternative.QueryCandidate;
import com.querylens.comparison.CandidateComparison;
import com.querylens.comparison.ComparisonReport;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkPublisher {
    private final List<BenchmarkObserver> observers = new ArrayList<>();

    public void subscribe(BenchmarkObserver observer) {
        observers.add(observer);
    }

    public void candidateStarted(QueryCandidate candidate, int position, int total) {
        observers.forEach(observer -> observer.onCandidateStarted(candidate, position, total));
    }

    public void candidateCompleted(CandidateComparison result) {
        observers.forEach(observer -> observer.onCandidateCompleted(result));
    }

    public void comparisonCompleted(ComparisonReport report) {
        observers.forEach(observer -> observer.onComparisonCompleted(report));
    }
}
