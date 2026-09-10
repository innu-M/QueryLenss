package com.querylens.workspace.model;

public record QueryHistoryEntry(long id, String sql, SqlQueryType type, long durationMillis, String executedAt) { }
