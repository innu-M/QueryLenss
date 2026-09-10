package com.querylens.workspace;

import java.util.List;
import com.querylens.recommendation.Recommendation;

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
