package com.querylens.persistence.comparison;

import com.querylens.history.model.ComparisonCandidateDraft;
import com.querylens.history.model.ComparisonCandidateEntry;
import com.querylens.history.model.ComparisonDraft;
import com.querylens.history.model.ComparisonHistorySummary;
import com.querylens.history.model.ComparisonSessionEntry;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ComparisonHistoryRepository {
    private static final String SESSION_QUERY = """
            SELECT s.id, s.title, s.database_path, s.original_sql, s.ranking_strategy, s.created_at,
                   COUNT(c.id) AS candidate_count,
                   COALESCE(MAX(CASE WHEN c.rank_position = 1 THEN c.label END), '') AS winner_label,
                   COALESCE(MAX(CASE WHEN LOWER(c.label) = 'original' THEN c.median_ns END), 0) AS original_median,
                   COALESCE(MAX(CASE WHEN c.rank_position = 1 THEN c.median_ns END), 0) AS winner_median
              FROM comparison_sessions s
              LEFT JOIN comparison_candidates c ON c.comparison_id = s.id
             WHERE (? = '' OR LOWER(s.title) LIKE ? OR LOWER(s.original_sql) LIKE ? OR LOWER(s.database_path) LIKE ?)
             GROUP BY s.id
             ORDER BY s.created_at DESC, s.id DESC
            """;

    private final Path databasePath;

    public ComparisonHistoryRepository(Path databasePath) {
        this.databasePath = databasePath.toAbsolutePath();
    }

    public long save(ComparisonDraft draft) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                long sessionId = insertSession(connection, draft);
                for (ComparisonCandidateDraft candidate : draft.candidates()) {
                    long candidateId = insertCandidate(connection, sessionId, candidate);
                    insertRuns(connection, candidateId, candidate.durationSamplesNs());
                }
                connection.commit();
                return sessionId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceFailure("save comparison history", exception);
        }
    }

    public List<ComparisonSessionEntry> findSessions(String searchText) {
        String normalized = searchText == null ? "" : searchText.strip().toLowerCase();
        String pattern = "%" + normalized + "%";
        List<ComparisonSessionEntry> sessions = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(SESSION_QUERY)) {
            statement.setString(1, normalized);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    sessions.add(new ComparisonSessionEntry(
                            result.getLong("id"),
                            result.getString("title"),
                            result.getString("database_path"),
                            result.getString("original_sql"),
                            result.getString("ranking_strategy"),
                            result.getString("created_at"),
                            result.getInt("candidate_count"),
                            result.getString("winner_label"),
                            result.getLong("original_median"),
                            result.getLong("winner_median")));
                }
            }
            return sessions;
        } catch (SQLException exception) {
            throw persistenceFailure("read comparison history", exception);
        }
    }

    public List<ComparisonCandidateEntry> findCandidates(long comparisonId) {
        String sql = """
                SELECT c.*, COUNT(r.id) AS sample_count
                  FROM comparison_candidates c
                  LEFT JOIN benchmark_runs r ON r.candidate_id = c.id
                 WHERE c.comparison_id = ?
                 GROUP BY c.id
                 ORDER BY CASE WHEN c.rank_position = 0 THEN 1 ELSE 0 END, c.rank_position, c.id
                """;
        List<ComparisonCandidateEntry> candidates = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, comparisonId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    candidates.add(new ComparisonCandidateEntry(
                            result.getLong("id"),
                            result.getLong("comparison_id"),
                            result.getString("label"),
                            result.getString("sql_text"),
                            result.getLong("median_ns"),
                            result.getLong("p95_ns"),
                            result.getInt("equivalent") == 1,
                            result.getString("status"),
                            result.getInt("rank_position"),
                            result.getString("explanation"),
                            result.getString("plan_text"),
                            result.getInt("sample_count")));
                }
            }
            return candidates;
        } catch (SQLException exception) {
            throw persistenceFailure("read comparison candidates", exception);
        }
    }

    public List<Long> findRunDurations(long candidateId) {
        String sql = "SELECT duration_ns FROM benchmark_runs WHERE candidate_id = ? ORDER BY run_number";
        List<Long> durations = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, candidateId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) durations.add(result.getLong("duration_ns"));
            }
            return durations;
        } catch (SQLException exception) {
            throw persistenceFailure("read benchmark runs", exception);
        }
    }

    public ComparisonHistorySummary summarize() {
        List<ComparisonSessionEntry> sessions = findSessions("");
        long verifiedCandidates = countVerifiedCandidates();
        List<Double> improvements = sessions.stream()
                .filter(session -> session.originalMedianNs() > 0 && session.winnerMedianNs() > 0)
                .map(ComparisonSessionEntry::improvementPercent)
                .toList();
        double average = improvements.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double best = improvements.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return new ComparisonHistorySummary(sessions.size(), verifiedCandidates, average, best);
    }

    public void delete(long comparisonId) {
        String sql = "DELETE FROM comparison_sessions WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, comparisonId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("delete comparison history", exception);
        }
    }

    public void rename(long comparisonId, String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Enter a comparison title.");
        String sql = "UPDATE comparison_sessions SET title = ? WHERE id = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title.strip());
            statement.setLong(2, comparisonId);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Comparison session not found.");
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw persistenceFailure("rename comparison history", exception);
        }
    }

    private long insertSession(Connection connection, ComparisonDraft draft) throws SQLException {
        String sql = "INSERT INTO comparison_sessions(title, database_path, original_sql, ranking_strategy) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, defaultTitle(draft.originalSql()));
            statement.setString(2, draft.databasePath());
            statement.setString(3, draft.originalSql());
            statement.setString(4, draft.rankingStrategy().name());
            statement.executeUpdate();
            return generatedId(statement, "comparison session");
        }
    }

    private String defaultTitle(String sql) {
        String compact = sql == null ? "Comparison" : sql.strip().replaceAll("\\s+", " ");
        if (compact.isBlank()) return "Comparison";
        return compact.length() <= 60 ? compact : compact.substring(0, 57) + "...";
    }

    private long insertCandidate(Connection connection, long sessionId, ComparisonCandidateDraft candidate)
            throws SQLException {
        String sql = """
                INSERT INTO comparison_candidates(
                    comparison_id, label, sql_text, median_ns, p95_ns, equivalent,
                    status, rank_position, explanation, plan_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, sessionId);
            statement.setString(2, candidate.label());
            statement.setString(3, candidate.sql());
            statement.setLong(4, candidate.medianNs());
            statement.setLong(5, candidate.p95Ns());
            statement.setInt(6, candidate.equivalent() ? 1 : 0);
            statement.setString(7, candidate.status());
            statement.setInt(8, candidate.rank());
            statement.setString(9, candidate.explanation());
            statement.setString(10, candidate.planText());
            statement.executeUpdate();
            return generatedId(statement, "comparison candidate");
        }
    }

    private void insertRuns(Connection connection, long candidateId, List<Long> samples) throws SQLException {
        String sql = "INSERT INTO benchmark_runs(candidate_id, run_number, duration_ns) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < samples.size(); index++) {
                statement.setLong(1, candidateId);
                statement.setInt(2, index + 1);
                statement.setLong(3, samples.get(index));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private long countVerifiedCandidates() {
        String sql = "SELECT COUNT(*) FROM comparison_candidates WHERE equivalent = 1";
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getLong(1) : 0;
        } catch (SQLException exception) {
            throw persistenceFailure("summarize comparison history", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private long generatedId(PreparedStatement statement, String recordName) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) return keys.getLong(1);
        }
        throw new SQLException("No ID was generated for the " + recordName + ".");
    }

    private IllegalStateException persistenceFailure(String operation, SQLException cause) {
        return new IllegalStateException("Could not " + operation + ".", cause);
    }
}
