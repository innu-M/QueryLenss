package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import com.querylens.model.AnalysisResultBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WhereColumnStrategyTest {
    @Test
    void recommendsIndexWhenSQLiteUsesFullScan() {
        AnalysisResult result = new AnalysisResultBuilder()
                .queryType("SELECT").tables(List.of("test")).whereColumns(List.of("cat"))
                .planSteps(List.of("SCAN test")).build();
        assertTrue(new WhereColumnStrategy().recommend(result).isPresent());
    }

    @Test
    void skipsIndexRecommendationWhenColumnIsAlreadyIndexed() {
        AnalysisResult result = new AnalysisResultBuilder()
                .queryType("SELECT").tables(List.of("test")).whereColumns(List.of("cat"))
                .planSteps(List.of("SEARCH test USING INDEX idx_cat (cat=?)"))
                .indexedWhereColumns(List.of("cat")).build();
        assertTrue(new WhereColumnStrategy().recommend(result).isEmpty());
    }
}
