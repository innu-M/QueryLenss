package com.querylens.alternative;

import java.util.List;

public interface AlternativeQueryStrategy {
    List<GeneratedQueryCandidate> generate(QueryStructure query, List<String> indexes);
}
