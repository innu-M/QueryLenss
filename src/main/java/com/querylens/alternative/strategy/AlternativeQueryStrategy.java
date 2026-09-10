package com.querylens.alternative.strategy;

import com.querylens.alternative.model.GeneratedQueryCandidate;
import com.querylens.alternative.model.QueryStructure;

import java.util.List;

public interface AlternativeQueryStrategy {
    List<GeneratedQueryCandidate> generate(QueryStructure query, List<String> indexes);
}
