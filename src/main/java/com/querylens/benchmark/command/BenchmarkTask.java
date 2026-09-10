package com.querylens.benchmark.command;

@FunctionalInterface
public interface BenchmarkTask {
    void execute() throws Exception;
}
