package com.querylens.workspace.analysis;

import com.querylens.workspace.model.SqlQueryType;

import java.util.Locale;

public final class SqlClassifier {
    public SqlQueryType classify(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Enter a SQL statement.");
        }
        String keyword = sql.stripLeading().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        try {
            return SqlQueryType.valueOf(keyword);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("QueryLens supports SELECT, INSERT, UPDATE, and DELETE statements.");
        }
    }
}
