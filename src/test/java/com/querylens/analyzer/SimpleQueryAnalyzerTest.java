package com.querylens.analyzer;

import com.querylens.model.AnalysisResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleQueryAnalyzerTest {

    private final SimpleQueryAnalyzer analyzer = new SimpleQueryAnalyzer();

    @Test
    void identifiesCommonSelectQueryDetails() {
        AnalysisResult result = analyzer.analyze("SELECT * FROM sailors s JOIN reserves r ON s.sid = r.sid WHERE s.rating = 8");

        assertEquals("SELECT", result.queryType());
        assertEquals(1, result.joinCount());
        assertTrue(result.usesSelectStar());
        assertTrue(result.hasWhereClause());
        assertTrue(result.tables().contains("sailors"));
        assertTrue(result.whereColumns().contains("rating"));
    }
}
