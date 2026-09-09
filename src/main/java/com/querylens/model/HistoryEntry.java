package com.querylens.model;

public record HistoryEntry(
        long id,
        String databasePath,
        String sql,
        long executionTimeMs,
        String status,
        String reviewStatus,
        String executedAt
) {
}
