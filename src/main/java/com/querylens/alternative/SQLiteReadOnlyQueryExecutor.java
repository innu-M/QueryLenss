package com.querylens.alternative;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class SQLiteReadOnlyQueryExecutor {
    public String fingerprint(Path databasePath, String sql, Duration timeout) {
        try (Connection connection = connect(databasePath);
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSeconds(timeout));
            try (ResultSet result = statement.executeQuery(sql)) {
                ResultSetMetaData metadata = result.getMetaData();
                List<String> rows = new ArrayList<>();
                while (result.next()) rows.add(rowValue(result, metadata));
                if (!sql.toUpperCase(Locale.ROOT).contains("ORDER BY")) Collections.sort(rows);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(metadataValue(metadata).getBytes(StandardCharsets.UTF_8));
                rows.forEach(row -> digest.update(row.getBytes(StandardCharsets.UTF_8)));
                return HexFormat.of().formatHex(digest.digest());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify candidate results.", exception);
        }
    }

    public void executeAndConsume(Path databasePath, String sql, Duration timeout) {
        try (PreparedSelect select = prepare(databasePath, sql, timeout)) {
            select.execute();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not execute a benchmark candidate.", exception);
        }
    }

    public PreparedSelect prepare(Path databasePath, String sql, Duration timeout) {
        try {
            Connection connection = connect(databasePath);
            try {
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setQueryTimeout(timeoutSeconds(timeout));
                return new PreparedSelect(connection, statement);
            } catch (Exception exception) {
                connection.close();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not prepare a benchmark candidate.", exception);
        }
    }

    private Connection connect(Path databasePath) throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA query_only = ON");
        }
        return connection;
    }

    private int timeoutSeconds(Duration timeout) {
        return Math.max(1, Math.toIntExact(Math.min(Integer.MAX_VALUE, timeout.toSeconds())));
    }

    private String metadataValue(ResultSetMetaData metadata) throws Exception {
        StringBuilder value = new StringBuilder().append(metadata.getColumnCount()).append('|');
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            append(value, metadata.getColumnLabel(column));
            append(value, metadata.getColumnTypeName(column));
        }
        return value.toString();
    }

    private String rowValue(ResultSet result, ResultSetMetaData metadata) throws Exception {
        StringBuilder row = new StringBuilder();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            Object value = result.getObject(column);
            append(row, value == null ? "<NULL>" : value.getClass().getName() + ":" + value);
        }
        return row.toString();
    }

    private void append(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value).append('|');
    }

    public static final class PreparedSelect implements AutoCloseable {
        private final Connection connection;
        private final PreparedStatement statement;

        private PreparedSelect(Connection connection, PreparedStatement statement) {
            this.connection = connection;
            this.statement = statement;
        }

        public void execute() {
            try (ResultSet result = statement.executeQuery()) {
                int columns = result.getMetaData().getColumnCount();
                while (result.next()) {
                    for (int column = 1; column <= columns; column++) result.getObject(column);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not execute a prepared benchmark candidate.", exception);
            }
        }

        @Override
        public void close() {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
