package com.querylens.sandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public final class SandboxIndexTester {
    public SandboxIndexResult test(Path sourceDatabase, String selectSql, ProposedIndex proposal) {
        validateSelectQuery(selectSql);
        Path sandbox = null;
        try {
            sandbox = createSandbox(sourceDatabase);
            QuerySample original = measure(sourceDatabase, selectSql);
            applyIndex(sandbox, proposal);
            QuerySample indexed = measure(sandbox, selectSql);
            return new SandboxIndexResult(
                    original.elapsedNanos(),
                    indexed.elapsedNanos(),
                    original.fingerprint().equals(indexed.fingerprint())
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not complete the sandbox index test.", exception);
        } finally {
            deleteSandbox(sandbox);
        }
    }

    private void validateSelectQuery(String selectSql) {
        if (!selectSql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("Sandbox tests accept SELECT queries only.");
        }
    }

    private Path createSandbox(Path sourceDatabase) throws Exception {
        Path sandbox = Files.createTempFile("querylens-index-sandbox-", ".db");
        Files.copy(sourceDatabase, sandbox, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return sandbox;
    }

    private void applyIndex(Path sandbox, ProposedIndex proposal) throws Exception {
        try (Connection connection = connect(sandbox);
             Statement statement = connection.createStatement()) {
            statement.execute(proposal.createStatement());
        }
    }

    private QuerySample measure(Path database, String selectSql) throws Exception {
        StringBuilder fingerprint = new StringBuilder();
        long started = System.nanoTime();
        try (Connection connection = connect(database);
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(selectSql)) {
            int columns = results.getMetaData().getColumnCount();
            while (results.next()) {
                for (int column = 1; column <= columns; column++) {
                    fingerprint.append(results.getObject(column)).append('|');
                }
                fingerprint.append('\n');
            }
        }
        return new QuerySample(System.nanoTime() - started, fingerprint.toString());
    }

    private Connection connect(Path database) throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    private void deleteSandbox(Path sandbox) {
        if (sandbox == null) {
            return;
        }
        try {
            Files.deleteIfExists(sandbox);
        } catch (Exception ignored) {
        }
    }

    private record QuerySample(long elapsedNanos, String fingerprint) {
    }
}
