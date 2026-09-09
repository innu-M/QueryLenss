package com.querylens.analyzer;

public class SimpleQueryAnalyzer extends QueryAnalysisTemplate {
    @Override
    protected int additionalComplexity(String sql) {
        return 0;
    }
}
