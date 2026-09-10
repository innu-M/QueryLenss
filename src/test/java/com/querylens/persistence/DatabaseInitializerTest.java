package com.querylens.persistence;

import com.querylens.persistence.core.DatabaseInitializer;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerTest {

    @Test
    void createsTheQueryLensSchema() throws Exception {
        Path database = Files.createTempFile("querylens-schema-", ".db");
        new DatabaseInitializer().initialize(database);

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.prepareStatement(
                     "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'recommendations'")) {
            assertTrue(statement.executeQuery().next());
        } finally {
            Files.deleteIfExists(database);
        }
    }
}
