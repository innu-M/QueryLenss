package com.querylens.benchmark.model;

public record BenchmarkProgress(String candidateLabel, int completedRuns, int totalRuns, Status status) {
    public enum Status { WARMING_UP, MEASURING, COMPLETED, CANCELLED, TIMED_OUT, FAILED }
}
