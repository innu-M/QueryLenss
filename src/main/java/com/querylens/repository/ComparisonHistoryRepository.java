package com.querylens.repository;

import com.querylens.comparison.CandidateComparison;
import com.querylens.comparison.ComparisonReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class ComparisonHistoryRepository {
    public double calculatePercentile(String databasePath, String normalizedQuery, long durationNs) {
        String sql = """
                SELECT cc.median_duration_ns
                FROM candidate_comparisons cc
                JOIN comparison_sessions cs ON cs.comparison_id = cc.comparison_id
                WHERE cs.database_path = ? AND cs.normalized_query = ?
                  AND cc.status = 'VERIFIED' AND cc.equivalent = 1
                """;
        long total = 0;
        long slower = 0;
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, databasePath);
            statement.setString(2, normalizedQuery);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    total++;
                    if (rows.getLong("median_duration_ns") > durationNs) slower++;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not calculate the local performance percentile.", exception);
        }
        return total == 0 ? Double.NaN : 100.0 * slower / total;
    }

    public void save(ComparisonReport report) {
        try (var connection = DatabaseManager.openHistoryConnection()) {
            connection.setAutoCommit(false);
            try {
                long comparisonId = insertSession(connection, report);
                for (CandidateComparison candidate : report.candidates()) {
                    long candidateId = insertCandidate(connection, comparisonId, candidate);
                    insertRuns(connection, candidateId, candidate);
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save the comparison report.", exception);
        }
    }

    private long insertSession(Connection connection, ComparisonReport report) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO comparison_sessions (database_path, original_sql, normalized_query)
                VALUES (?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, report.databasePath());
            statement.setString(2, report.originalSql());
            statement.setString(3, report.normalizedQuery());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Could not read the saved comparison ID.");
    }

    private long insertCandidate(Connection connection, long comparisonId, CandidateComparison candidate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO candidate_comparisons
                    (comparison_id, label, candidate_sql, rationale, median_duration_ns, equivalent,
                     status, rank_position, percentile, explanation, plan_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, comparisonId);
            statement.setString(2, candidate.candidate().label());
            statement.setString(3, candidate.candidate().sql());
            statement.setString(4, candidate.candidate().rationale());
            statement.setLong(5, candidate.medianDurationNs());
            statement.setBoolean(6, candidate.equivalent());
            statement.setString(7, candidate.status());
            statement.setInt(8, candidate.rank());
            if (Double.isNaN(candidate.percentile())) statement.setNull(9, java.sql.Types.REAL);
            else statement.setDouble(9, candidate.percentile());
            statement.setString(10, candidate.explanation());
            statement.setString(11, String.join("\n", candidate.planSteps()));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Could not read the saved candidate ID.");
    }

    private void insertRuns(Connection connection, long candidateId, CandidateComparison candidate) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO benchmark_runs (candidate_id, run_number, duration_ns) VALUES (?, ?, ?)
                """)) {
            for (int index = 0; index < candidate.durationSamplesNs().size(); index++) {
                statement.setLong(1, candidateId);
                statement.setInt(2, index + 1);
                statement.setLong(3, candidate.durationSamplesNs().get(index));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
