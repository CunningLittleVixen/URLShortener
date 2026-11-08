package io.github.cunninglittlevixen.urlshortener.infra;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CleanupScheduler {

    private static final long STOP_TIMEOUT_SECONDS = 5;

    private final Duration interval;
    private final Runnable task;
    private ScheduledExecutorService executor;

    public CleanupScheduler(Duration interval, Runnable task) {
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(task, "task");
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("Интервал очистки должен быть положительным");
        }
        this.interval = interval;
        this.task = task;
    }

    public synchronized void start() {
        if (executor != null) {
            throw new IllegalStateException("Планировщик уже запущен");
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cleanup-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::runSafely, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executor = null;
    }

    private void runSafely() {
        try {
            task.run();
        } catch (RuntimeException e) {
            System.err.println("Ошибка автоочистки: " + e.getMessage());
        }
    }
}
