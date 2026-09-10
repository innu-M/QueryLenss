package com.querylens.alternative;

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
