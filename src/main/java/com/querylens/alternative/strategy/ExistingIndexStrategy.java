package com.querylens.alternative.strategy;

import com.querylens.alternative.model.GeneratedQueryCandidate;
import com.querylens.alternative.model.QueryStructure;

import java.util.List;

public final class ExistingIndexStrategy implements AlternativeQueryStrategy {
    @Override
    public List<GeneratedQueryCandidate> generate(QueryStructure query, List<String> indexes) {
        return indexes.stream().map(index -> new GeneratedQueryCandidate(
                "Index: " + index,
                query.addTableDirective("INDEXED BY " + quote(index)),
                "Forces existing index " + index + " so QueryLens can measure it against SQLite's default plan."))
                .toList();
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
