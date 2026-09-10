package com.querylens.alternative.model;

import java.util.List;

public record AlternativeCompetitionResult(
        long comparisonId,
        List<CompetitionCandidate> candidates,
        CompetitionCandidate winner) {

    public AlternativeCompetitionResult {
        candidates = List.copyOf(candidates);
    }
}
