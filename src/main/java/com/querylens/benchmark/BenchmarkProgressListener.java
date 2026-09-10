package com.querylens.benchmark;

@FunctionalInterface
public interface BenchmarkProgressListener {
    void onProgress(BenchmarkProgress progress);
}
