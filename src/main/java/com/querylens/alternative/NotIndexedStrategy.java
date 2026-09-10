package com.querylens.alternative;

import java.util.List;

public final class NotIndexedStrategy implements AlternativeQueryStrategy {
    @Override
    public List<GeneratedQueryCandidate> generate(QueryStructure query, List<String> indexes) {
        return List.of(new GeneratedQueryCandidate(
                "Table scan",
                query.addTableDirective("NOT INDEXED"),
                "Forces a table scan so its measured cost can be compared with SQLite's chosen access path."));
    }
}
