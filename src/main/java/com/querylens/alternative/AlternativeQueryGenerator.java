package com.querylens.alternative;

import com.querylens.model.AnalysisResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlternativeQueryGenerator {
    private final List<QueryRewriteStrategy> strategies;

    public AlternativeQueryGenerator() {
        this(new QueryRewriteStrategyFactory().createStrategies());
    }

    public AlternativeQueryGenerator(List<QueryRewriteStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public List<QueryCandidate> generate(Connection connection, String sql, AnalysisResult analysis) throws SQLException {
        Map<String, QueryCandidate> uniqueBySql = new LinkedHashMap<>();
        QueryCandidate original = new QueryCandidate("Original", sql.trim(),
                "The query exactly as entered; it is the baseline for every comparison.");
        uniqueBySql.put(normalize(original.sql()), original);

        for (QueryRewriteStrategy strategy : strategies) {
            if (!strategy.supports(sql, analysis)) continue;
            for (QueryCandidate candidate : strategy.generate(connection, sql, analysis)) {
                uniqueBySql.putIfAbsent(normalize(candidate.sql()), candidate);
            }
        }
        return new ArrayList<>(uniqueBySql.values());
    }

    private String normalize(String sql) {
        return sql.trim().replaceAll(";+$", "").replaceAll("\\s+", " ").toUpperCase();
    }
}
