package com.querylens.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not prepare the QueryLens database.", exception);
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
