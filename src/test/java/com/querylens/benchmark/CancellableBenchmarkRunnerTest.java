package com.querylens.benchmark;

import com.querylens.benchmark.model.BenchmarkProgress;
import com.querylens.benchmark.model.BenchmarkSettings;
import com.querylens.benchmark.model.RankingStrategy;
import com.querylens.benchmark.service.CancellableBenchmarkRunner;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellableBenchmarkRunnerTest {
    private final BenchmarkSettings settings = new BenchmarkSettings(0, 2, Duration.ofMillis(50), 3, RankingStrategy.MEDIAN);

    @Test
    void reportsCancellationBeforeMeasuring() {
        AtomicReference<BenchmarkProgress.Status> status = new AtomicReference<>();
        try (CancellableBenchmarkRunner runner = new CancellableBenchmarkRunner()) {
            runner.cancel();
            assertTrue(runner.run("candidate", () -> { }, settings, progress -> status.set(progress.status())).isEmpty());
        }
        assertEquals(BenchmarkProgress.Status.CANCELLED, status.get());
    }

    @Test
    void reportsTimeoutAndReturnsNoIncompleteSample() {
        AtomicReference<BenchmarkProgress.Status> status = new AtomicReference<>();
        try (CancellableBenchmarkRunner runner = new CancellableBenchmarkRunner()) {
            assertTrue(runner.run("candidate", () -> Thread.sleep(200), settings, progress -> status.set(progress.status())).isEmpty());
        }
        assertEquals(BenchmarkProgress.Status.TIMED_OUT, status.get());
    }
}
