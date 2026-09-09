package com.querylens.observer;

import com.querylens.comparison.ComparisonReport;
import com.querylens.repository.ComparisonHistoryRepository;

public class BenchmarkPersistenceObserver implements BenchmarkObserver {
    private final ComparisonHistoryRepository repository;

    public BenchmarkPersistenceObserver(ComparisonHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onComparisonCompleted(ComparisonReport report) {
        repository.save(report);
    }
}
