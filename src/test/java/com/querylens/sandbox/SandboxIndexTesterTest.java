package com.querylens.sandbox;

import com.querylens.sandbox.model.ProposedIndex;
import com.querylens.sandbox.model.SandboxIndexResult;
import com.querylens.sandbox.service.SandboxIndexTester;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxIndexTesterTest {
    @Test
    void appliesTheIndexOnlyToATemporaryCopy() throws Exception {
        Path database = Files.createTempFile("querylens-source-", ".db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE events (id INTEGER PRIMARY KEY, category TEXT)");
            statement.execute("INSERT INTO events(category) VALUES ('study'), ('work'), ('study')");
        }

        SandboxIndexResult result = new SandboxIndexTester().test(database,
                "SELECT id, category FROM events WHERE category = 'study'", new ProposedIndex("idx_events_category", "events", "category"));

        assertTrue(result.resultsMatch());
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = 'idx_events_category'")) {
            assertFalse(statement.executeQuery().next());
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
