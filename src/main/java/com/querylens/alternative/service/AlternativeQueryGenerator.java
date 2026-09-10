package com.querylens.alternative.service;

import com.querylens.alternative.adapter.IndexCatalogProvider;
import com.querylens.alternative.model.GeneratedQueryCandidate;
import com.querylens.alternative.model.QueryStructure;
import com.querylens.alternative.parser.SafeSelectParser;
import com.querylens.alternative.strategy.AlternativeQueryStrategy;
import com.querylens.alternative.strategy.AlternativeQueryStrategyFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AlternativeQueryGenerator implements QueryCandidateGenerator {
    private final SafeSelectParser parser;
    private final IndexCatalogProvider indexCatalog;
    private final List<AlternativeQueryStrategy> strategies;

    public AlternativeQueryGenerator(IndexCatalogProvider indexCatalog) {
        this(new SafeSelectParser(), indexCatalog, new AlternativeQueryStrategyFactory().standardStrategies());
    }

    AlternativeQueryGenerator(SafeSelectParser parser, IndexCatalogProvider indexCatalog,
                              List<AlternativeQueryStrategy> strategies) {
        this.parser = parser;
        this.indexCatalog = indexCatalog;
        this.strategies = List.copyOf(strategies);
    }

    @Override
    public List<GeneratedQueryCandidate> generate(Path databasePath, String sql, int maximumCandidates) {
        if (databasePath == null || !Files.isRegularFile(databasePath)) {
            throw new IllegalArgumentException("Choose an existing SQLite database file.");
        }
        if (maximumCandidates < 1) throw new IllegalArgumentException("Maximum candidates must be positive.");
        QueryStructure query = parser.parse(sql);
        List<String> indexes = indexCatalog.findIndexes(databasePath, query.tableName());
        Map<String, GeneratedQueryCandidate> unique = new LinkedHashMap<>();
        GeneratedQueryCandidate original = new GeneratedQueryCandidate(
                "Original", query.sql(), "SQLite chooses the default execution plan.");
        unique.put(original.sql(), original);
        for (AlternativeQueryStrategy strategy : strategies) {
            for (GeneratedQueryCandidate candidate : strategy.generate(query, indexes)) {
                unique.putIfAbsent(candidate.sql(), candidate);
                if (unique.size() >= maximumCandidates) return List.copyOf(unique.values());
            }
        }
        return new ArrayList<>(unique.values());
    }
}
