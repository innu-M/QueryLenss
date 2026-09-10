package com.querylens.plan.adapter;

import com.querylens.plan.model.QueryPlanRow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SQLiteQueryPlanInspector implements QueryPlanProvider {
    @Override
    public List<QueryPlanRow> inspect(Path databasePath, String selectSql) {
        validate(databasePath, selectSql);
        String normalizedSql = removeTrailingSemicolon(selectSql.strip());
        List<QueryPlanRow> rows = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
             Statement settings = connection.createStatement()) {
            settings.execute("PRAGMA query_only = ON");
            try (PreparedStatement statement = connection.prepareStatement("EXPLAIN QUERY PLAN " + normalizedSql);
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new QueryPlanRow(
                            result.getInt("id"),
                            result.getInt("parent"),
                            result.getString("detail")));
                }
            }
            return List.copyOf(rows);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not inspect the SQLite query plan.", exception);
        }
    }

    private void validate(Path databasePath, String sql) {
        if (databasePath == null || !Files.isRegularFile(databasePath)) {
            throw new IllegalArgumentException("Choose an existing SQLite database file.");
        }
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Enter a SELECT query to inspect.");
        }
        String withoutTrailing = removeTrailingSemicolon(sql.strip());
        if (withoutTrailing.contains(";")) {
            throw new IllegalArgumentException("Only one SQL statement can be inspected at a time.");
        }
        String upper = withoutTrailing.toUpperCase(Locale.ROOT);
        if (!(upper.startsWith("SELECT ") || upper.startsWith("SELECT\n") || upper.startsWith("WITH ") ||
                upper.startsWith("WITH\n"))) {
            throw new IllegalArgumentException("Plan visualization accepts SELECT queries only.");
        }
    }

    private String removeTrailingSemicolon(String sql) {
        return sql.endsWith(";") ? sql.substring(0, sql.length() - 1).stripTrailing() : sql;
    }
}
