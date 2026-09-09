package com.querylens.comparison;

import java.util.List;

public interface RankingStrategy {
    List<CandidateComparison> rank(List<CandidateComparison> candidates);
}
