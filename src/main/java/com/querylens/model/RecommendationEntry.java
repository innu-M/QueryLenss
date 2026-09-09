package com.querylens.model;

public record RecommendationEntry(long id, long queryId, String type, String description, String priority, String status) {
}
