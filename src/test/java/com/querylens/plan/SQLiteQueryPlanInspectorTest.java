package com.querylens.plan;

import com.querylens.plan.adapter.SQLiteQueryPlanInspector;
import com.querylens.plan.model.QueryPlanRow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteQueryPlanInspectorTest {
    @TempDir
    Path temporaryDirectory;

    private Path database;
    private final SQLiteQueryPlanInspector inspector = new SQLiteQueryPlanInspector();

    @BeforeEach
    void setUp() throws Exception {
        database = temporaryDirectory.resolve("demo.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sailors (sid INTEGER PRIMARY KEY, name TEXT, rating INTEGER)");
            statement.execute("CREATE INDEX idx_sailors_rating ON sailors(rating)");
            statement.execute("INSERT INTO sailors(name, rating) VALUES ('Irin', 10), ('Munni', 8)");
        }
    }

    @Test
    void readsRealSqlitePlanIdsParentsAndDetails() {
        List<QueryPlanRow> rows = inspector.inspect(database, "SELECT name FROM sailors WHERE rating = 10;");

        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().anyMatch(row -> row.detail().contains("idx_sailors_rating")));
    }

    @Test
    void rejectsWritesAndMultipleStatements() {
        assertThrows(IllegalArgumentException.class,
                () -> inspector.inspect(database, "DELETE FROM sailors"));
        assertThrows(IllegalArgumentException.class,
                () -> inspector.inspect(database, "SELECT * FROM sailors; SELECT 1"));
    }
}
