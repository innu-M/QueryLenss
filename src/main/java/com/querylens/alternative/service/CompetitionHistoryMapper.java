package com.querylens.alternative.service;

import com.querylens.alternative.model.CompetitionCandidate;
import com.querylens.benchmark.model.BenchmarkSettings;
import com.querylens.history.model.ComparisonCandidateDraft;
import com.querylens.history.model.ComparisonDraft;

import java.nio.file.Path;
import java.util.List;

/** Maps benchmark-domain results into persistence drafts. */
public final class CompetitionHistoryMapper {
    public ComparisonDraft toDraft(Path databasePath, String originalSql, BenchmarkSettings settings,
                                   List<CompetitionCandidate> candidates) {
        List<ComparisonCandidateDraft> drafts = candidates.stream()
                .map(candidate -> new ComparisonCandidateDraft(
                        candidate.label(), candidate.sql(), candidate.samplesNs(), candidate.equivalent(),
                        candidate.status(), candidate.rank(), candidate.explanation(), candidate.planText()))
                .toList();
        return new ComparisonDraft(databasePath.toAbsolutePath().toString(), originalSql,
                settings.rankingStrategy(), drafts);
    }
}
