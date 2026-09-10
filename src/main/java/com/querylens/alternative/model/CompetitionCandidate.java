package com.querylens.alternative.model;

import java.util.List;

public record CompetitionCandidate(
        String label,
        String sql,
        List<Long> samplesNs,
        long medianNs,
        long p95Ns,
        boolean equivalent,
        String status,
        int rank,
        double beatsPercent,
        String explanation,
        String planText) {

    public CompetitionCandidate {
        samplesNs = List.copyOf(samplesNs);
    }
}
