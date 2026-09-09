package com.querylens.observer;

import com.querylens.alternative.QueryCandidate;
import com.querylens.comparison.CandidateComparison;
import com.querylens.comparison.ComparisonReport;

public interface BenchmarkObserver {
    default void onCandidateStarted(QueryCandidate candidate, int position, int total) {
    }

    default void onCandidateCompleted(CandidateComparison result) {
    }

    default void onComparisonCompleted(ComparisonReport report) {
    }
}
