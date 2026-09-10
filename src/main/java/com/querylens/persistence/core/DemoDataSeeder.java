package com.querylens.persistence.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class DemoDataSeeder {
    private static final String DEMO_NAME = "QueryLens Demo";

    public Path seed(Path workspaceDatabase) {
        Path absoluteWorkspace = workspaceDatabase.toAbsolutePath();
        Path parent = absoluteWorkspace.getParent();
        Path demoDatabase = (parent == null ? Path.of("querylens-demo.db") : parent.resolve("querylens-demo.db"))
                .toAbsolutePath();
        try {
            if (demoDatabase.getParent() != null) Files.createDirectories(demoDatabase.getParent());
            initializeDemoDatabase(demoDatabase);
            registerConnection(absoluteWorkspace, demoDatabase);
            return demoDatabase;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not seed the QueryLens demonstration data.", exception);
        }
    }

    private void initializeDemoDatabase(Path demoDatabase) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + demoDatabase);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            executeScript(statement, readResource("/database/demo-schema.sql"));
            executeScript(statement, readResource("/database/demo-data.sql"));
        }
    }

    private void registerConnection(Path workspaceDatabase, Path demoDatabase) throws Exception {
        String sql = """
                INSERT INTO database_connections(display_name, database_path)
                VALUES (?, ?)
                ON CONFLICT(database_path) DO UPDATE SET display_name = excluded.display_name
                """;
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + workspaceDatabase);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DEMO_NAME);
            statement.setString(2, demoDatabase.toString());
            statement.executeUpdate();
        }
    }

    private void executeScript(Statement statement, String script) throws Exception {
        for (String sql : script.split(";")) {
            if (!sql.isBlank()) statement.execute(sql);
        }
    }

    private String readResource(String resource) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing database resource: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
