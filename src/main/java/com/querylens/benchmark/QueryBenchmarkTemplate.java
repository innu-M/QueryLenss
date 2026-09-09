package com.querylens.benchmark;

import com.querylens.alternative.QueryCandidate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class QueryBenchmarkTemplate {
    private final int warmUpRuns;
    private final int measuredRuns;

    protected QueryBenchmarkTemplate(int warmUpRuns, int measuredRuns) {
        if (warmUpRuns < 0 || measuredRuns < 1) throw new IllegalArgumentException("Invalid benchmark run count.");
        this.warmUpRuns = warmUpRuns;
        this.measuredRuns = measuredRuns;
    }

    public final RawBenchmarkResult benchmark(Connection connection, QueryCandidate candidate) throws SQLException {
        prepare(connection);
        ResultFingerprint fingerprint = null;
        for (int run = 0; run < warmUpRuns; run++) fingerprint = execute(connection, candidate.sql()).fingerprint();

        List<Long> samples = new ArrayList<>();
        for (int run = 0; run < measuredRuns; run++) {
            MeasuredExecution execution = execute(connection, candidate.sql());
            if (fingerprint != null && !fingerprint.equals(execution.fingerprint())) {
                throw new SQLException("The candidate returned inconsistent results between benchmark runs.");
            }
            fingerprint = execution.fingerprint();
            samples.add(execution.durationNs());
        }
        List<Long> ordered = samples.stream().sorted(Comparator.naturalOrder()).toList();
        long median = ordered.get(ordered.size() / 2);
        return new RawBenchmarkResult(candidate, List.copyOf(samples), median, fingerprint,
                List.copyOf(readPlan(connection, candidate.sql())));
    }

    protected void prepare(Connection connection) throws SQLException {
    }

    protected abstract MeasuredExecution execute(Connection connection, String sql) throws SQLException;

    protected abstract List<String> readPlan(Connection connection, String sql) throws SQLException;

    protected record MeasuredExecution(long durationNs, ResultFingerprint fingerprint) {
    }
}
