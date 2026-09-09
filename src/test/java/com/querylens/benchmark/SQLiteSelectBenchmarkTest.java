package com.querylens.benchmark;

import com.querylens.alternative.QueryCandidate;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SQLiteSelectBenchmarkTest {
    @Test
    void measuresRowsAndCapturesARealPlan() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sailors (sid INTEGER PRIMARY KEY, rating INTEGER)");
            statement.execute("INSERT INTO sailors VALUES (1, 8), (2, 7), (3, 8)");

            var result = new SQLiteSelectBenchmark(0, 3).benchmark(connection,
                    new QueryCandidate("Original", "SELECT sid FROM sailors WHERE rating = 8", "Baseline"));

            assertEquals(3, result.durationSamplesNs().size());
            assertEquals(2, result.fingerprint().rowCount());
            assertFalse(result.planSteps().isEmpty());
        }
    }
}
