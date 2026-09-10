package com.querylens.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CancellableBenchmarkRunner implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public List<Long> run(String label, BenchmarkTask task, BenchmarkSettings settings, BenchmarkProgressListener listener) {
        List<Long> samples = new ArrayList<>();
        try {
            if (!runWarmups(label, task, settings, listener)) {
                return List.of();
            }
            if (runMeasurements(label, task, settings, listener, samples)) {
                return List.copyOf(samples);
            }
            report(label, samples.size(), settings.measuredRuns(), BenchmarkProgress.Status.COMPLETED, listener);
            return List.copyOf(samples);
        } catch (TimeoutException exception) {
            report(label, samples.size(), settings.measuredRuns(), BenchmarkProgress.Status.TIMED_OUT, listener);
            return List.copyOf(samples);
        } catch (Exception exception) {
            report(label, samples.size(), settings.measuredRuns(), BenchmarkProgress.Status.FAILED, listener);
            throw new IllegalStateException("Benchmark task failed.", exception);
        }
    }

    public void cancel() {
        cancelled.set(true);
    }

    private boolean runWarmups(String label,
                               BenchmarkTask task,
                               BenchmarkSettings settings,
                               BenchmarkProgressListener listener) throws Exception {
        for (int warmup = 1; warmup <= settings.warmupRuns(); warmup++) {
            if (cancelled.get()) {
                report(label, warmup - 1, settings.warmupRuns(), BenchmarkProgress.Status.CANCELLED, listener);
                return false;
            }
            report(label, warmup - 1, settings.warmupRuns(), BenchmarkProgress.Status.WARMING_UP, listener);
            execute(task, settings);
        }
        return true;
    }

    private boolean runMeasurements(String label,
                                    BenchmarkTask task,
                                    BenchmarkSettings settings,
                                    BenchmarkProgressListener listener,
                                    List<Long> samples) throws Exception {
        for (int run = 1; run <= settings.measuredRuns(); run++) {
            if (cancelled.get()) {
                report(label, run - 1, settings.measuredRuns(), BenchmarkProgress.Status.CANCELLED, listener);
                return true;
            }
            report(label, run - 1, settings.measuredRuns(), BenchmarkProgress.Status.MEASURING, listener);
            samples.add(measure(task, settings));
        }
        return false;
    }

    private long measure(BenchmarkTask task, BenchmarkSettings settings) throws Exception {
        long started = System.nanoTime();
        execute(task, settings);
        return System.nanoTime() - started;
    }

    private void report(String label,
                        int completedRuns,
                        int totalRuns,
                        BenchmarkProgress.Status status,
                        BenchmarkProgressListener listener) {
        listener.onProgress(new BenchmarkProgress(label, completedRuns, totalRuns, status));
    }

    private void execute(BenchmarkTask task, BenchmarkSettings settings) throws Exception {
        Future<Void> future = executor.submit((Callable<Void>) () -> {
            task.execute();
            return null;
        });
        try {
            future.get(settings.queryTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw exception;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}

