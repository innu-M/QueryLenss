package com.querylens.workspace.model;

import java.util.List;
import com.querylens.recommendation.model.Recommendation;

public record QueryExecutionResult(
        boolean returnsRows,
        List<String> columns,
        List<List<String>> rows,
        int affectedRows,
        long durationMillis,
        QueryAnalysis analysis,
        List<Recommendation> recommendations) {

    public QueryExecutionResult {
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
        recommendations = List.copyOf(recommendations);
    }
}
