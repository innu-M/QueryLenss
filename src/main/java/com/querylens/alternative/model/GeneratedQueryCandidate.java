package com.querylens.alternative.model;

public record GeneratedQueryCandidate(String label, String sql, String explanation) {
    public GeneratedQueryCandidate {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Candidate label is required.");
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("Candidate SQL is required.");
        explanation = explanation == null ? "" : explanation;
    }
}
