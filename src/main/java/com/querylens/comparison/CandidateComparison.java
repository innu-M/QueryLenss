package com.querylens.comparison;

import com.querylens.alternative.QueryCandidate;

import java.util.List;

public record CandidateComparison(
        QueryCandidate candidate,
        List<Long> durationSamplesNs,
        long medianDurationNs,
        List<String> planSteps,
        boolean equivalent,
        String status,
        int rank,
        double percentile,
        String explanation
) {
    public double medianMilliseconds() {
        return medianDurationNs / 1_000_000.0;
    }

    public CandidateComparison withRank(int value) {
        return new CandidateComparison(candidate, durationSamplesNs, medianDurationNs, planSteps, equivalent,
                status, value, percentile, explanation);
    }

    public CandidateComparison withPercentile(double value) {
        return new CandidateComparison(candidate, durationSamplesNs, medianDurationNs, planSteps, equivalent,
                status, rank, value, explanation);
    }

    public CandidateComparison withExplanation(String value) {
        return new CandidateComparison(candidate, durationSamplesNs, medianDurationNs, planSteps, equivalent,
                status, rank, percentile, value);
    }
}
