package com.querylens.service;

import com.querylens.alternative.AlternativeQueryGenerator;
import com.querylens.alternative.QueryCandidate;
import com.querylens.analyzer.SimpleQueryAnalyzer;
import com.querylens.benchmark.RawBenchmarkResult;
import com.querylens.benchmark.ResultFingerprint;
import com.querylens.benchmark.SQLiteSelectBenchmark;
import com.querylens.comparison.CandidateComparison;
import com.querylens.comparison.ComparisonReport;
import com.querylens.comparison.MedianTimeRankingStrategy;
import com.querylens.comparison.PlanExplanationService;
import com.querylens.comparison.RankingStrategy;
import com.querylens.comparison.SqlNormalizer;
import com.querylens.observer.BenchmarkObserver;
import com.querylens.observer.BenchmarkPersistenceObserver;
import com.querylens.observer.BenchmarkPublisher;
import com.querylens.repository.ComparisonHistoryRepository;
import com.querylens.repository.DatabaseManager;
import com.querylens.validation.CandidateValidationChain;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlternativeComparisonService {
    private final SimpleQueryAnalyzer analyzer = new SimpleQueryAnalyzer();
    private final AlternativeQueryGenerator generator = new AlternativeQueryGenerator();
    private final CandidateValidationChain validators = new CandidateValidationChain();
    private final SQLiteSelectBenchmark benchmark = new SQLiteSelectBenchmark();
    private final RankingStrategy ranking = new MedianTimeRankingStrategy();
    private final PlanExplanationService explanations = new PlanExplanationService();
    private final SqlNormalizer normalizer = new SqlNormalizer();
    private final BenchmarkPublisher publisher = new BenchmarkPublisher();
    private final ComparisonHistoryRepository historyRepository;

    public AlternativeComparisonService() {
        this(new ComparisonHistoryRepository());
    }

    public AlternativeComparisonService(ComparisonHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
        publisher.subscribe(new BenchmarkPersistenceObserver(historyRepository));
    }

    public void subscribe(BenchmarkObserver observer) {
        publisher.subscribe(observer);
    }

    public ComparisonReport compare(String databasePath, String sql) throws SQLException {
        if (databasePath == null || databasePath.isBlank()) throw new IllegalArgumentException("Enter a SQLite database path.");
        QueryCandidate requested = new QueryCandidate("Original", sql == null ? "" : sql.trim(), "Baseline query.");
        var validation = validators.validate(requested);
        if (!validation.valid()) throw new IllegalArgumentException(validation.message());

        List<CandidateComparison> comparisons = new ArrayList<>();
        ResultFingerprint originalFingerprint = null;
        try (var connection = DatabaseManager.openTargetConnection(databasePath)) {
            var analysis = analyzer.analyze(sql);
            List<QueryCandidate> candidates = generator.generate(connection, sql, analysis);
            for (int index = 0; index < candidates.size(); index++) {
                QueryCandidate candidate = candidates.get(index);
                publisher.candidateStarted(candidate, index + 1, candidates.size());
                var candidateValidation = validators.validate(candidate);
                if (!candidateValidation.valid()) {
                    comparisons.add(failed(candidate, "REJECTED", candidateValidation.message()));
                    continue;
                }
                try {
                    RawBenchmarkResult raw = benchmark.benchmark(connection, candidate);
                    if (originalFingerprint == null) originalFingerprint = raw.fingerprint();
                    boolean equivalent = originalFingerprint.equals(raw.fingerprint());
                    CandidateComparison comparison = new CandidateComparison(candidate, raw.durationSamplesNs(),
                            raw.medianDurationNs(), raw.planSteps(), equivalent,
                            equivalent ? "VERIFIED" : "NOT_EQUIVALENT", 0, Double.NaN, "");
                    comparisons.add(comparison);
                    publisher.candidateCompleted(comparison);
                } catch (SQLException exception) {
                    if (index == 0) throw exception;
                    comparisons.add(failed(candidate, "FAILED", exception.getMessage()));
                }
            }
        }

        comparisons = ranking.rank(comparisons);
        String normalizedQuery = normalizer.normalize(sql);
        comparisons = comparisons.stream()
                .map(candidate -> eligibleForPercentile(candidate)
                        ? candidate.withPercentile(historyRepository.calculatePercentile(
                                databasePath, normalizedQuery, candidate.medianDurationNs()))
                        : candidate)
                .toList();
        CandidateComparison original = comparisons.getFirst();
        List<CandidateComparison> explained = comparisons.stream()
                .map(candidate -> candidate.withExplanation(explanations.explain(original, candidate)))
                .toList();
        ComparisonReport report = new ComparisonReport(databasePath, sql, normalizedQuery, explained);
        publisher.comparisonCompleted(report);
        return report;
    }

    private boolean eligibleForPercentile(CandidateComparison candidate) {
        return candidate.equivalent() && "VERIFIED".equals(candidate.status());
    }

    private CandidateComparison failed(QueryCandidate candidate, String status, String message) {
        return new CandidateComparison(candidate, List.of(), 0, List.of(), false, status, 0,
                Double.NaN, message == null ? status : message);
    }
}
