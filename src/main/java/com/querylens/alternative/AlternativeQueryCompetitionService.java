package com.querylens.alternative;

import com.querylens.benchmark.BenchmarkProgress;
import com.querylens.benchmark.BenchmarkProgressListener;
import com.querylens.benchmark.BenchmarkSettings;
import com.querylens.benchmark.CancellableBenchmarkRunner;
import com.querylens.benchmark.CandidateMetrics;
import com.querylens.history.ComparisonCandidateDraft;
import com.querylens.history.ComparisonDraft;
import com.querylens.persistence.ComparisonHistoryRepository;
import com.querylens.plan.QueryPlanProvider;
import com.querylens.plan.QueryPlanRow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AlternativeQueryCompetitionService {
    private final QueryCandidateGenerator generator;
    private final SQLiteReadOnlyQueryExecutor executor;
    private final QueryPlanProvider planProvider;
    private final ComparisonHistoryRepository historyRepository;
    private volatile CancellableBenchmarkRunner activeRunner;

    public AlternativeQueryCompetitionService(
            QueryCandidateGenerator generator,
            SQLiteReadOnlyQueryExecutor executor,
            QueryPlanProvider planProvider,
            ComparisonHistoryRepository historyRepository) {
        this.generator = generator;
        this.executor = executor;
        this.planProvider = planProvider;
        this.historyRepository = historyRepository;
    }

    public AlternativeCompetitionResult compete(Path databasePath, String originalSql,
                                                 BenchmarkSettings settings,
                                                 BenchmarkProgressListener listener) {
        List<GeneratedQueryCandidate> generated = generator.generate(
                databasePath, originalSql, settings.maximumCandidates());
        String originalFingerprint = executor.fingerprint(
                databasePath, generated.getFirst().sql(), settings.queryTimeout());
        List<CandidateWork> measured = new ArrayList<>();
        CancellableBenchmarkRunner runner = new CancellableBenchmarkRunner();
        activeRunner = runner;
        try (runner) {
            for (GeneratedQueryCandidate candidate : generated) {
                boolean equivalent = originalFingerprint.equals(
                        executor.fingerprint(databasePath, candidate.sql(), settings.queryTimeout()));
                AtomicReference<BenchmarkProgress.Status> finalStatus =
                        new AtomicReference<>(equivalent ? BenchmarkProgress.Status.MEASURING : BenchmarkProgress.Status.FAILED);
                List<Long> samples = List.of();
                if (equivalent) {
                    try (SQLiteReadOnlyQueryExecutor.PreparedSelect prepared = executor.prepare(
                            databasePath, candidate.sql(), settings.queryTimeout())) {
                        samples = runner.run(candidate.label(),
                                prepared::execute,
                                settings,
                                progress -> {
                                    finalStatus.set(progress.status());
                                    listener.onProgress(progress);
                                });
                    } catch (IllegalStateException exception) {
                        finalStatus.set(BenchmarkProgress.Status.FAILED);
                    }
                }
                String status = equivalent ? status(finalStatus.get(), samples) : "REJECTED_NOT_EQUIVALENT";
                measured.add(new CandidateWork(candidate, samples, equivalent, status,
                        planText(planProvider.inspect(databasePath, candidate.sql()))));
            }
        } finally {
            activeRunner = null;
        }

        List<CompetitionCandidate> ranked = rank(measured, settings);
        long comparisonId = historyRepository.save(toDraft(databasePath, originalSql, settings, ranked));
        CompetitionCandidate winner = ranked.stream().filter(candidate -> candidate.rank() == 1).findFirst().orElse(null);
        return new AlternativeCompetitionResult(comparisonId, ranked, winner);
    }

    public void cancel() {
        CancellableBenchmarkRunner runner = activeRunner;
        if (runner != null) runner.cancel();
    }

    private List<CompetitionCandidate> rank(List<CandidateWork> candidates, BenchmarkSettings settings) {
        List<CandidateWork> eligible = candidates.stream()
                .filter(candidate -> candidate.equivalent() && !candidate.samples().isEmpty())
                .sorted(Comparator.comparing(
                        candidate -> new CandidateMetrics(candidate.generated().label(), candidate.samples()),
                        CandidateMetrics.comparatorFor(settings.rankingStrategy())))
                .toList();
        Map<String, Integer> ranks = new HashMap<>();
        for (int index = 0; index < eligible.size(); index++) ranks.put(eligible.get(index).generated().sql(), index + 1);
        long originalMedian = candidates.stream()
                .filter(candidate -> candidate.generated().label().equals("Original") && !candidate.samples().isEmpty())
                .mapToLong(candidate -> new CandidateMetrics(candidate.generated().label(), candidate.samples()).medianNanos())
                .findFirst().orElse(0);

        List<CompetitionCandidate> results = new ArrayList<>();
        for (CandidateWork candidate : candidates) {
            CandidateMetrics metrics = candidate.samples().isEmpty()
                    ? null : new CandidateMetrics(candidate.generated().label(), candidate.samples());
            int rank = ranks.getOrDefault(candidate.generated().sql(), 0);
            double beats = eligible.size() <= 1 || rank == 0 ? 0
                    : 100.0 * (eligible.size() - rank) / (eligible.size() - 1);
            long median = metrics == null ? 0 : metrics.medianNanos();
            String explanation = measuredExplanation(candidate.generated().explanation(), median, originalMedian, rank);
            results.add(new CompetitionCandidate(
                    candidate.generated().label(), candidate.generated().sql(), candidate.samples(), median,
                    metrics == null ? 0 : metrics.p95Nanos(), candidate.equivalent(), candidate.status(), rank,
                    beats, explanation, candidate.planText()));
        }
        return List.copyOf(results);
    }

    private String measuredExplanation(String base, long median, long originalMedian, int rank) {
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

    private ComparisonDraft toDraft(Path databasePath, String originalSql, BenchmarkSettings settings,
                                    List<CompetitionCandidate> candidates) {
        List<ComparisonCandidateDraft> drafts = candidates.stream().map(candidate -> new ComparisonCandidateDraft(
                candidate.label(), candidate.sql(), candidate.samplesNs(), candidate.equivalent(), candidate.status(),
                candidate.rank(), candidate.explanation(), candidate.planText())).toList();
        return new ComparisonDraft(databasePath.toAbsolutePath().toString(), originalSql,
                settings.rankingStrategy(), drafts);
    }

    private String status(BenchmarkProgress.Status status, List<Long> samples) {
        if (status == BenchmarkProgress.Status.COMPLETED) return "VERIFIED";
        if (status == BenchmarkProgress.Status.TIMED_OUT) return "TIMED_OUT";
        if (status == BenchmarkProgress.Status.CANCELLED) return "CANCELLED";
        return samples.isEmpty() ? "FAILED" : "PARTIAL";
    }

    private String planText(List<QueryPlanRow> rows) {
        return rows.stream().map(row -> "[%d → %d] %s".formatted(row.id(), row.parentId(), row.detail()))
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private record CandidateWork(GeneratedQueryCandidate generated, List<Long> samples,
                                 boolean equivalent, String status, String planText) {
    }
}
