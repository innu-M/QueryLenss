package com.querylens.model;

public record IndexEntry(String tableName, String indexName, String columnName, boolean unique) {
}
