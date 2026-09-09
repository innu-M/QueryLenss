package com.querylens.comparison;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class MedianTimeRankingStrategy implements RankingStrategy {
    @Override
    public List<CandidateComparison> rank(List<CandidateComparison> candidates) {
        List<CandidateComparison> eligible = candidates.stream()
                .filter(CandidateComparison::equivalent)
                .filter(candidate -> "VERIFIED".equals(candidate.status()))
                .sorted(Comparator.comparingLong(CandidateComparison::medianDurationNs))
                .toList();
        Map<CandidateComparison, Integer> ranks = new IdentityHashMap<>();
        for (int index = 0; index < eligible.size(); index++) ranks.put(eligible.get(index), index + 1);

        List<CandidateComparison> ranked = new ArrayList<>();
        for (CandidateComparison candidate : candidates) {
            ranked.add(candidate.withRank(ranks.getOrDefault(candidate, 0)));
        }
        return ranked;
    }
}
