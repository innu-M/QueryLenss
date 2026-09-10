package com.querylens.workspace;

public record QueryHistoryEntry(long id, String sql, SqlQueryType type, long durationMillis, String executedAt) { }
