package com.querylens.service;

import com.querylens.analyzer.SimpleQueryAnalyzer;
import com.querylens.model.AnalysisResult;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteQueryPlanInspectorTest {

    @Test
    void detectsAnExistingIndexForAWhereColumn() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sailors (sid INTEGER PRIMARY KEY, rating INTEGER)");
            statement.execute("CREATE INDEX idx_sailors_rating ON sailors(rating)");

            AnalysisResult initial = new SimpleQueryAnalyzer().analyze("SELECT * FROM sailors WHERE rating = 8");
            AnalysisResult inspected = new SQLiteQueryPlanInspector().inspect(connection,
                    "SELECT * FROM sailors WHERE rating = 8", initial);

            assertTrue(inspected.indexedWhereColumns().contains("rating"));
            assertTrue(inspected.planSteps().stream().anyMatch(step -> step.contains("INDEX")));
        }
    }
}
