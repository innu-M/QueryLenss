package com.querylens.service;

import com.querylens.model.AnalysisResult;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteQueryPlanInspector implements QueryPlanProvider {

    public AnalysisResult inspect(Connection connection, String sql, AnalysisResult analysis) {
        List<String> planSteps = readPlan(connection, sql, analysis.queryType());
        List<String> indexedColumns = new ArrayList<>();

        if (!analysis.tables().isEmpty()) {
            String table = analysis.tables().getFirst();
            for (String column : analysis.whereColumns()) {
                if (hasIndexForColumn(connection, table, column)) indexedColumns.add(column);
            }
        }
        return analysis.withExecutionDetails(planSteps, indexedColumns);
    }

    private List<String> readPlan(Connection connection, String sql, String queryType) {
        if (!"SELECT".equals(queryType)) return List.of("EXPLAIN QUERY PLAN is shown for SELECT queries.");
        List<String> steps = new ArrayList<>();
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("EXPLAIN QUERY PLAN " + sql)) {
            while (result.next()) steps.add(result.getString("detail"));
        } catch (SQLException exception) {
            return List.of("Plan unavailable: " + exception.getMessage());
        }
        return steps.isEmpty() ? List.of("No plan steps returned.") : steps;
    }

    private boolean hasIndexForColumn(Connection connection, String table, String column) {
        try {
            if (isPrimaryKey(connection, table, column)) return true;
            try (var statement = connection.createStatement();
                 ResultSet indexes = statement.executeQuery("PRAGMA index_list('" + table + "')")) {
                while (indexes.next()) {
                    if (indexContainsColumn(connection, indexes.getString("name"), column)) return true;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    private boolean isPrimaryKey(Connection connection, String table, String column) throws SQLException {
        try (var statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info('" + table + "')")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name")) && columns.getInt("pk") > 0) return true;
            }
        }
        return false;
    }

    private boolean indexContainsColumn(Connection connection, String indexName, String column) throws SQLException {
        try (var statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA index_info('" + indexName + "')")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) return true;
            }
        }
        return false;
    }
}
