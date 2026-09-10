package com.querylens.workspace.execution.template;
import com.querylens.workspace.SqlQueryType;


import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class QueryExecutionTemplate {
    private static final int MAX_ROWS = 100;

    public final RawQueryResult execute(Path databasePath, String sql, SqlQueryType type) {
        long started = System.nanoTime();
        try (Connection connection = openConnection(databasePath);
             Statement statement = connection.createStatement()) {
            if (type == SqlQueryType.SELECT) {
                return readRows(statement.executeQuery(sql), elapsedMillis(started));
            }
            int affectedRows = statement.executeUpdate(sql);
            return new RawQueryResult(false, List.of(), List.of(), affectedRows, elapsedMillis(started));
        } catch (Exception exception) {
            throw new IllegalStateException("Query execution failed: " + exception.getMessage(), exception);
        }
    }

    protected abstract Connection openConnection(Path databasePath) throws Exception;

    private RawQueryResult readRows(ResultSet resultSet, long durationMillis) throws Exception {
        try (resultSet) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            List<String> columns = readColumns(metadata);
            List<List<String>> rows = readRows(resultSet, metadata.getColumnCount());
            return new RawQueryResult(true, columns, rows, 0, durationMillis);
        }
    }

    private List<String> readColumns(ResultSetMetaData metadata) throws Exception {
        List<String> columns = new ArrayList<>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
            columns.add(metadata.getColumnLabel(column));
        }
        return columns;
    }

    private List<List<String>> readRows(ResultSet resultSet, int columnCount) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        while (resultSet.next() && rows.size() < MAX_ROWS) {
            rows.add(readRow(resultSet, columnCount));
        }
        return rows;
    }

    private List<String> readRow(ResultSet resultSet, int columnCount) throws Exception {
        List<String> row = new ArrayList<>();
        for (int column = 1; column <= columnCount; column++) {
            row.add(String.valueOf(resultSet.getObject(column)));
        }
        return row;
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    public record RawQueryResult(boolean returnsRows,
                                 List<String> columns,
                                 List<List<String>> rows,
                                 int affectedRows,
                                 long durationMillis) {
    }
}




