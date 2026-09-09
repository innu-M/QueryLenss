package com.querylens.alternative;

import com.querylens.analyzer.SimpleQueryAnalyzer;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlternativeQueryGeneratorTest {
    @Test
    void generatesExistingIndexAndTableScanCandidates() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sailors (sid INTEGER PRIMARY KEY, rating INTEGER)");
            statement.execute("CREATE INDEX idx_sailors_rating ON sailors(rating)");
            String sql = "SELECT sid FROM sailors WHERE rating = 8";

            var candidates = new AlternativeQueryGenerator().generate(connection, sql,
                    new SimpleQueryAnalyzer().analyze(sql));

            assertEquals("Original", candidates.getFirst().label());
            assertTrue(candidates.stream().anyMatch(candidate -> candidate.sql().contains("NOT INDEXED")));
            assertTrue(candidates.stream().anyMatch(candidate -> candidate.sql().contains("INDEXED BY \"idx_sailors_rating\"")));
        }
    }
}
