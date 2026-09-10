package com.querylens.benchmark;

@FunctionalInterface
public interface BenchmarkTask {
    void execute() throws Exception;
}
