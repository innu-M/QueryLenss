package com.querylens.model;

import java.util.List;

public record QueryExecutionResult(
        String databasePath,
        String sql,
        List<String> columnNames,
        List<List<String>> rows,
        long executionTimeMs,
        String status,
        boolean slow,
        AnalysisResult analysis,
        List<Recommendation> recommendations
) {
}
