package com.querylens.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {

    private static final String HISTORY_DATABASE_URL = "jdbc:sqlite:data/querylens-history.db";

    private DatabaseManager() {
    }

    public static void initializeHistoryDatabase() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(HISTORY_DATABASE_URL);
                 var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS database_connections (
                            connection_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            database_path TEXT NOT NULL UNIQUE,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS query_history (
                            query_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            database_path TEXT NOT NULL,
                            sql_query TEXT NOT NULL,
                            execution_time_ms INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            review_status TEXT NOT NULL DEFAULT 'NEW',
                            executed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS query_analysis (
                            analysis_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            query_id INTEGER NOT NULL UNIQUE,
                            query_type TEXT NOT NULL,
                            tables_used TEXT,
                            join_count INTEGER NOT NULL,
                            complexity_score INTEGER NOT NULL,
                            risk_level TEXT NOT NULL,
                            FOREIGN KEY (query_id) REFERENCES query_history(query_id)
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS recommendations (
                            recommendation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            query_id INTEGER NOT NULL,
                            recommendation_type TEXT NOT NULL,
                            description TEXT NOT NULL,
                            priority TEXT NOT NULL,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            FOREIGN KEY (query_id) REFERENCES query_history(query_id)
                        )
                        """);
                ensureRecommendationStatusColumn(connection);
                ensureHistoryReviewColumn(connection);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS index_information (
                            index_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            table_name TEXT NOT NULL,
                            column_name TEXT NOT NULL,
                            index_type TEXT,
                            usage_count INTEGER NOT NULL DEFAULT 0,
                            UNIQUE (table_name, column_name)
                        )
                        """);
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_query_history_time ON query_history(execution_time_ms)");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS comparison_sessions (
                            comparison_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            database_path TEXT NOT NULL,
                            original_sql TEXT NOT NULL,
                            normalized_query TEXT NOT NULL,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS candidate_comparisons (
                            candidate_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            comparison_id INTEGER NOT NULL,
                            label TEXT NOT NULL,
                            candidate_sql TEXT NOT NULL,
                            rationale TEXT NOT NULL,
                            median_duration_ns INTEGER NOT NULL,
                            equivalent INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            rank_position INTEGER NOT NULL,
                            percentile REAL,
                            explanation TEXT NOT NULL,
                            plan_text TEXT NOT NULL,
                            FOREIGN KEY (comparison_id) REFERENCES comparison_sessions(comparison_id) ON DELETE CASCADE
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS benchmark_runs (
                            benchmark_run_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            candidate_id INTEGER NOT NULL,
                            run_number INTEGER NOT NULL,
                            duration_ns INTEGER NOT NULL,
                            FOREIGN KEY (candidate_id) REFERENCES candidate_comparisons(candidate_id) ON DELETE CASCADE
                        )
                        """);
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_comparison_query ON comparison_sessions(database_path, normalized_query)");
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialize QueryLens history database.", exception);
        }
    }

    public static Connection openHistoryConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(HISTORY_DATABASE_URL);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        return connection;
    }

    public static Connection openTargetConnection(String databasePath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private static void ensureRecommendationStatusColumn(Connection connection) throws SQLException {
        if (!hasColumn(connection, "recommendations", "status")) {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE recommendations ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'");
            }
        }
    }

    private static void ensureHistoryReviewColumn(Connection connection) throws SQLException {
        if (!hasColumn(connection, "query_history", "review_status")) {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE query_history ADD COLUMN review_status TEXT NOT NULL DEFAULT 'NEW'");
            }
        }
    }

    private static boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (var statement = connection.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("name"))) return true;
            }
        }
        return false;
    }
}
