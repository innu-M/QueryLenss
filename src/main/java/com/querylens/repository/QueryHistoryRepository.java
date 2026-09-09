package com.querylens.repository;

import com.querylens.model.HistoryEntry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QueryHistoryRepository {

    public long save(String databasePath, String sql, long executionTimeMs, String status) {
        String statement = """
                INSERT INTO query_history (database_path, sql_query, execution_time_ms, status)
                VALUES (?, ?, ?, ?)
                """;

        try (var connection = DatabaseManager.openHistoryConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, databasePath);
            preparedStatement.setString(2, sql);
            preparedStatement.setLong(3, executionTimeMs);
            preparedStatement.setString(4, status);
            preparedStatement.executeUpdate();
            try (var keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save query history.", exception);
        }
        throw new IllegalStateException("Could not read the saved query ID.");
    }

    public List<HistoryEntry> findRecent() {
        return find("");
    }

    public List<HistoryEntry> find(String searchText) {
        String sql = """
                SELECT query_id, database_path, sql_query, execution_time_ms, status, review_status, executed_at
                FROM query_history
                WHERE sql_query LIKE ? OR status LIKE ? OR database_path LIKE ?
                ORDER BY query_id DESC LIMIT 100
                """;
        List<HistoryEntry> entries = new ArrayList<>();
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement(sql);
        ) {
            String search = "%" + searchText.trim() + "%";
            statement.setString(1, search);
            statement.setString(2, search);
            statement.setString(3, search);
            try (var result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new HistoryEntry(result.getLong("query_id"), result.getString("database_path"),
                        result.getString("sql_query"), result.getLong("execution_time_ms"),
                        result.getString("status"), result.getString("review_status"), result.getString("executed_at")));
            }
            return entries;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load query history.", exception);
        }
    }

    public void delete(long id) {
        try (var connection = DatabaseManager.openHistoryConnection()) {
            connection.setAutoCommit(false);
            try (var recommendations = connection.prepareStatement("DELETE FROM recommendations WHERE query_id = ?");
                 var analysis = connection.prepareStatement("DELETE FROM query_analysis WHERE query_id = ?");
                 var history = connection.prepareStatement("DELETE FROM query_history WHERE query_id = ?")) {
                recommendations.setLong(1, id);
                recommendations.executeUpdate();
                analysis.setLong(1, id);
                analysis.executeUpdate();
                history.setLong(1, id);
                history.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete history record.", exception);
        }
    }

    public void markReviewed(long id) {
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement("UPDATE query_history SET review_status = 'REVIEWED' WHERE query_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update history review status.", exception);
        }
    }

    public String performanceSummary() {
        String sql = """
                SELECT COUNT(*) AS total, COALESCE(AVG(execution_time_ms), 0) AS average_time,
                       SUM(CASE WHEN status = 'SLOW' THEN 1 ELSE 0 END) AS slow_count
                FROM query_history
                """;
        try (var connection = DatabaseManager.openHistoryConnection();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            result.next();
            return "Total queries: " + result.getLong("total")
                    + "   |   Average time: " + String.format("%.1f", result.getDouble("average_time")) + " ms"
                    + "   |   Slow queries: " + result.getLong("slow_count");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load performance summary.", exception);
        }
    }
}
