package com.querylens.alternative.service;

import com.querylens.alternative.model.CompetitionCandidate;
import com.querylens.alternative.model.GeneratedQueryCandidate;
import com.querylens.benchmark.model.BenchmarkSettings;
import com.querylens.benchmark.model.CandidateMetrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ranks equivalent benchmark candidates and adds human-readable measured comparisons. */
public final class CandidateRanker {
    public List<CompetitionCandidate> rank(List<CandidateMeasurement> candidates, BenchmarkSettings settings) {
        List<CandidateMeasurement> eligible = candidates.stream()
                .filter(candidate -> candidate.equivalent() && !candidate.samples().isEmpty())
                .sorted(Comparator.comparing(
                        this::metrics,
                        CandidateMetrics.comparatorFor(settings.rankingStrategy())))
                .toList();
        Map<String, Integer> ranks = new HashMap<>();
        for (int index = 0; index < eligible.size(); index++) {
            ranks.put(eligible.get(index).generated().sql(), index + 1);
        }
        long originalMedian = candidates.stream()
                .filter(candidate -> candidate.generated().label().equals("Original"))
                .filter(candidate -> !candidate.samples().isEmpty())
                .mapToLong(candidate -> metrics(candidate).medianNanos())
                .findFirst()
                .orElse(0);

        List<CompetitionCandidate> results = new ArrayList<>();
        for (CandidateMeasurement candidate : candidates) {
            CandidateMetrics metrics = candidate.samples().isEmpty() ? null : metrics(candidate);
            int rank = ranks.getOrDefault(candidate.generated().sql(), 0);
            double beats = eligible.size() <= 1 || rank == 0
                    ? 0
                    : 100.0 * (eligible.size() - rank) / (eligible.size() - 1);
            long median = metrics == null ? 0 : metrics.medianNanos();
            results.add(new CompetitionCandidate(
                    candidate.generated().label(), candidate.generated().sql(), candidate.samples(), median,
                    metrics == null ? 0 : metrics.p95Nanos(), candidate.equivalent(), candidate.status(), rank,
                    beats, explain(candidate.generated().explanation(), median, originalMedian, rank),
                    candidate.planText()));
        }
        return List.copyOf(results);
    }

    private CandidateMetrics metrics(CandidateMeasurement candidate) {
        return new CandidateMetrics(candidate.generated().label(), candidate.samples());
    }

    private String explain(String base, long median, long originalMedian, int rank) {
        if (median <= 0 || originalMedian <= 0) return base;
        double improvement = 100.0 * (originalMedian - median) / originalMedian;
        if (rank == 1 && improvement > 0) {
            return base + " Measured median is %.1f%% faster than the original in this local run."
                    .formatted(improvement);
        }
        if (improvement < 0) {
            return base + " Measured median is %.1f%% slower than the original in this local run."
                    .formatted(-improvement);
        }
        return base + " Its measured median is close to the original; repeat with more runs before deciding.";
    }

    public record CandidateMeasurement(GeneratedQueryCandidate generated, List<Long> samples,
                                       boolean equivalent, String status, String planText) {
        public CandidateMeasurement {
            samples = List.copyOf(samples);
        }
    }
}
