package com.querylens.alternative;

import com.querylens.alternative.adapter.SQLiteBenchmarkExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SQLiteBenchmarkExecutorTest {
    @TempDir
    Path temporaryDirectory;

    private Path database;
    private final SQLiteBenchmarkExecutor executor = new SQLiteBenchmarkExecutor();

    @BeforeEach
    void setUp() throws Exception {
        database = temporaryDirectory.resolve("demo.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sailors(sid INTEGER PRIMARY KEY, name TEXT, rating INTEGER)");
            statement.execute("INSERT INTO sailors VALUES (1, 'Irin', 10), (2, 'Munni', 8)");
        }
    }

    @Test
    void fingerprintsEquivalentResultsIndependentOfPhysicalPlan() {
        String defaultPlan = executor.fingerprint(database, "SELECT * FROM sailors", Duration.ofSeconds(2));
        String tableScan = executor.fingerprint(database, "SELECT * FROM sailors NOT INDEXED", Duration.ofSeconds(2));

        assertEquals(defaultPlan, tableScan);
    }

    @Test
    void differentResultsHaveDifferentFingerprints() {
        String allRows = executor.fingerprint(database, "SELECT * FROM sailors", Duration.ofSeconds(2));
        String oneRow = executor.fingerprint(database, "SELECT * FROM sailors WHERE sid = 1", Duration.ofSeconds(2));

        assertNotEquals(allRows, oneRow);
    }
}
