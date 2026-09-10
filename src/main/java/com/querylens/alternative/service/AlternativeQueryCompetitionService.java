package com.querylens.alternative.service;

import com.querylens.alternative.adapter.SQLiteBenchmarkExecutor;
import com.querylens.alternative.model.AlternativeCompetitionResult;
import com.querylens.alternative.model.CompetitionCandidate;
import com.querylens.alternative.model.GeneratedQueryCandidate;
import com.querylens.benchmark.model.BenchmarkProgress;
import com.querylens.benchmark.observer.BenchmarkProgressListener;
import com.querylens.benchmark.model.BenchmarkSettings;
import com.querylens.benchmark.service.CancellableBenchmarkRunner;
import com.querylens.persistence.comparison.ComparisonHistoryRepository;
import com.querylens.plan.adapter.QueryPlanProvider;
import com.querylens.plan.model.QueryPlanRow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.querylens.alternative.service.CandidateRanker.CandidateMeasurement;

public final class AlternativeQueryCompetitionService {
    private final QueryCandidateGenerator generator;
    private final SQLiteBenchmarkExecutor executor;
    private final QueryPlanProvider planProvider;
    private final ComparisonHistoryRepository historyRepository;
    private final CandidateRanker candidateRanker;
    private final CompetitionHistoryMapper historyMapper;
    private volatile CancellableBenchmarkRunner activeRunner;

    public AlternativeQueryCompetitionService(
            QueryCandidateGenerator generator,
            SQLiteBenchmarkExecutor executor,
            QueryPlanProvider planProvider,
            ComparisonHistoryRepository historyRepository) {
        this(generator, executor, planProvider, historyRepository,
                new CandidateRanker(), new CompetitionHistoryMapper());
    }

    AlternativeQueryCompetitionService(
            QueryCandidateGenerator generator,
            SQLiteBenchmarkExecutor executor,
            QueryPlanProvider planProvider,
            ComparisonHistoryRepository historyRepository,
            CandidateRanker candidateRanker,
            CompetitionHistoryMapper historyMapper) {
        this.generator = generator;
        this.executor = executor;
        this.planProvider = planProvider;
        this.historyRepository = historyRepository;
        this.candidateRanker = candidateRanker;
        this.historyMapper = historyMapper;
    }

    public AlternativeCompetitionResult compete(Path databasePath, String originalSql,
                                                 BenchmarkSettings settings,
                                                 BenchmarkProgressListener listener) {
        List<GeneratedQueryCandidate> generated = generator.generate(
                databasePath, originalSql, settings.maximumCandidates());
        String originalFingerprint = executor.fingerprint(
                databasePath, generated.getFirst().sql(), settings.queryTimeout());
        List<CandidateMeasurement> measured = new ArrayList<>();
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
                    try (SQLiteBenchmarkExecutor.PreparedSelect prepared = executor.prepare(
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
                measured.add(new CandidateMeasurement(candidate, samples, equivalent, status,
                        planText(planProvider.inspect(databasePath, candidate.sql()))));
            }
        } finally {
            activeRunner = null;
        }

        List<CompetitionCandidate> ranked = candidateRanker.rank(measured, settings);
        long comparisonId = historyRepository.save(historyMapper.toDraft(databasePath, originalSql, settings, ranked));
        CompetitionCandidate winner = ranked.stream().filter(candidate -> candidate.rank() == 1).findFirst().orElse(null);
        return new AlternativeCompetitionResult(comparisonId, ranked, winner);
    }

    public void cancel() {
        CancellableBenchmarkRunner runner = activeRunner;
        if (runner != null) runner.cancel();
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

}
