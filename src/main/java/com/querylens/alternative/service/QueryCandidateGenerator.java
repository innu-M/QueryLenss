package com.querylens.alternative.service;

import com.querylens.alternative.model.GeneratedQueryCandidate;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface QueryCandidateGenerator {
    List<GeneratedQueryCandidate> generate(Path databasePath, String sql, int maximumCandidates);
}
