package com.querylens.sandbox.model;

public record ProposedIndex(String indexName, String tableName, String columnName) {
    public ProposedIndex {
        if (!isIdentifier(indexName) || !isIdentifier(tableName) || !isIdentifier(columnName)) {
            throw new IllegalArgumentException("Index names, table names, and column names must be simple SQL identifiers.");
        }
    }

    public String createStatement() {
        return "CREATE INDEX " + indexName + " ON " + tableName + " (" + columnName + ")";
    }

    private static boolean isIdentifier(String value) {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }
}
