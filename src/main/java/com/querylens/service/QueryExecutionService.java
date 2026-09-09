package com.querylens.service;

import com.querylens.analyzer.RecommendationEngine;
import com.querylens.analyzer.SimpleQueryAnalyzer;
import com.querylens.model.AnalysisResult;
import com.querylens.model.QueryExecutionResult;
import com.querylens.model.Recommendation;
import com.querylens.repository.DatabaseConnectionRepository;
import com.querylens.repository.DatabaseManager;
import com.querylens.observer.PersistenceObserver;
import com.querylens.observer.QueryExecutionPublisher;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutionService {

    private static final long SLOW_QUERY_THRESHOLD_MS = 500;
    private static final int MAX_DISPLAYED_ROWS = 100;

    private final DatabaseConnectionRepository connectionRepository = new DatabaseConnectionRepository();
    private final SimpleQueryAnalyzer queryAnalyzer = new SimpleQueryAnalyzer();
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();
    private final QueryPlanProvider queryPlanInspector = new SQLiteQueryPlanInspector();
    private final QueryExecutionPublisher publisher = new QueryExecutionPublisher();

    public QueryExecutionService() {
        publisher.subscribe(new PersistenceObserver());
    }

    public void initialize() {
        DatabaseManager.initializeHistoryDatabase();
    }

    public QueryExecutionResult execute(String databasePath, String sql) throws SQLException {
        if (databasePath.isBlank()) {
            throw new IllegalArgumentException("Enter a SQLite database path.");
        }
        if (sql.isBlank()) {
            throw new IllegalArgumentException("Enter a SQL query before executing it.");
        }

        connectionRepository.record(databasePath);
        AnalysisResult analysis = queryAnalyzer.analyze(sql);
        List<Recommendation> recommendations;
        long startedAt = System.nanoTime();
        List<String> columnNames = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        String status;

        try (var connection = DatabaseManager.openTargetConnection(databasePath);
             var statement = connection.createStatement()) {
            analysis = queryPlanInspector.inspect(connection, sql, analysis);
            recommendations = recommendationEngine.recommend(analysis);
            boolean returnsRows = statement.execute(sql);

            if (returnsRows) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    readRows(resultSet, columnNames, rows);
                }
                status = "Query completed (" + rows.size() + " row(s) shown)";
            } else {
                status = "Statement completed (" + statement.getUpdateCount() + " row(s) affected)";
            }
        }

        long executionTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
        boolean slow = executionTimeMs >= SLOW_QUERY_THRESHOLD_MS;
        QueryExecutionResult result = new QueryExecutionResult(databasePath, sql, columnNames, rows,
                executionTimeMs, status, slow, analysis, recommendations);
        publisher.publish(result);
        return result;
    }

    private void readRows(ResultSet resultSet, List<String> columnNames, List<List<String>> rows) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            columnNames.add(metadata.getColumnLabel(column));
        }

        while (resultSet.next() && rows.size() < MAX_DISPLAYED_ROWS) {
            List<String> row = new ArrayList<>();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                row.add(String.valueOf(resultSet.getObject(column)));
            }
            rows.add(row);
        }
    }
}
