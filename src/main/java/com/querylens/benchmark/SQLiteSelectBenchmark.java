package com.querylens.benchmark;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SQLiteSelectBenchmark extends QueryBenchmarkTemplate {
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    public SQLiteSelectBenchmark() {
        super(1, 5);
    }

    public SQLiteSelectBenchmark(int warmUpRuns, int measuredRuns) {
        super(warmUpRuns, measuredRuns);
    }

    @Override
    protected void prepare(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA query_only = ON");
        }
    }

    @Override
    protected MeasuredExecution execute(Connection connection, String sql) throws SQLException {
        long startedAt = System.nanoTime();
        int columnCount;
        long rowCount = 0;
        long rowHashSum = 0;
        long rowHashXor = 0;
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rows = statement.executeQuery(sql)) {
                columnCount = rows.getMetaData().getColumnCount();
                while (rows.next()) {
                    Object[] values = new Object[columnCount];
                    for (int column = 1; column <= columnCount; column++) values[column - 1] = rows.getObject(column);
                    long rowHash = Integer.toUnsignedLong(Arrays.deepHashCode(values));
                    rowHashSum += rowHash;
                    // Sum and XOR deliberately ignore row order because SQL results without ORDER BY
                    // have no guaranteed order. Row count keeps duplicate multiplicity visible.
                    rowHashXor ^= rowHash;
                    rowCount++;
                }
            }
        }
        long duration = System.nanoTime() - startedAt;
        return new MeasuredExecution(duration,
                new ResultFingerprint(columnCount, rowCount, rowHashSum, rowHashXor));
    }

    @Override
    protected List<String> readPlan(Connection connection, String sql) throws SQLException {
        List<String> steps = new ArrayList<>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("EXPLAIN QUERY PLAN " + sql)) {
            while (rows.next()) steps.add(rows.getString("detail"));
        }
        return steps;
    }
}
