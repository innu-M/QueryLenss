package com.querylens.benchmark.observer;

import com.querylens.benchmark.model.BenchmarkProgress;

@FunctionalInterface
public interface BenchmarkProgressListener {
    void onProgress(BenchmarkProgress progress);
}
