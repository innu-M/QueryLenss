package com.querylens.benchmark;

import com.querylens.alternative.QueryCandidate;

import java.util.List;

public record RawBenchmarkResult(
        QueryCandidate candidate,
        List<Long> durationSamplesNs,
        long medianDurationNs,
        ResultFingerprint fingerprint,
        List<String> planSteps
) {
}
