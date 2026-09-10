package com.querylens.sandbox.model;

public record SandboxIndexResult(long originalNanos, long indexedCopyNanos, boolean resultsMatch) {
    public double improvementPercent() {
        if (originalNanos <= 0) return 0;
        return ((originalNanos - indexedCopyNanos) * 100.0) / originalNanos;
    }
}
