package com.querylens.workspace.model;

import java.util.List;

public record QueryAnalysis(
        SqlQueryType queryType,
        List<String> tables,
        int joinCount,
        boolean hasWhereClause,
        boolean selectsAllColumns,
        List<String> filteredColumns,
        boolean hasOrderingOrGrouping,
        int complexityScore,
        String riskLevel) {

    public QueryAnalysis {
        tables = List.copyOf(tables);
        filteredColumns = List.copyOf(filteredColumns);
    }
}
