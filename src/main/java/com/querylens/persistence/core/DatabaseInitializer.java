package com.querylens.persistence.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseInitializer {

    public void initialize(Path databasePath) {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
                 Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                for (String sql : readSchema().split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
                addColumnIfMissing(connection, "comparison_sessions", "title",
                        "ALTER TABLE comparison_sessions ADD COLUMN title TEXT NOT NULL DEFAULT ''");
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not prepare the QueryLens database.", exception);
        }
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String migration)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(migration);
        }
    }

    private String readSchema() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/database/schema.sql")) {
            if (stream == null) {
                throw new IOException("Database schema resource is missing.");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
