package com.querylens.persistence;

import com.querylens.persistence.connection.ConnectionRepository;
import com.querylens.persistence.core.DatabaseInitializer;
import com.querylens.persistence.core.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataSeederTest {
    @TempDir
    Path directory;

    @Test
    void createsDemoDataAndCanRunRepeatedlyWithoutDuplicates() throws Exception {
        Path workspaceDatabase = directory.resolve("querylens.db");
        new DatabaseInitializer().initialize(workspaceDatabase);
        DemoDataSeeder seeder = new DemoDataSeeder();

        Path firstPath = seeder.seed(workspaceDatabase);
        Path secondPath = seeder.seed(workspaceDatabase);

        assertEquals(firstPath, secondPath);
        assertTrue(Files.isRegularFile(firstPath));
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + firstPath);
             var statement = connection.createStatement()) {
            assertEquals(5, count(statement, "sailors"));
            assertEquals(3, count(statement, "boats"));
            assertEquals(6, count(statement, "reserves"));
        }

        var savedConnections = new ConnectionRepository(workspaceDatabase).findAll();
        assertEquals(1, savedConnections.size());
        assertEquals("QueryLens Demo", savedConnections.getFirst().displayName());
        assertEquals(firstPath, savedConnections.getFirst().databasePath());
    }

    private long count(java.sql.Statement statement, String table) throws Exception {
        try (var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.getLong(1);
        }
    }
}
