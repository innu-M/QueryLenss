package com.querylens.alternative;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface QueryCandidateGenerator {
    List<GeneratedQueryCandidate> generate(Path databasePath, String sql, int maximumCandidates);
}
