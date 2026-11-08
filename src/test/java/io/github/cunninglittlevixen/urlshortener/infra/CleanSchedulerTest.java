package io.github.cunninglittlevixen.urlshortener.infra;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CleanupSchedulerTest {

    private static final Duration FAST = Duration.ofMillis(20);

    @Test
    void runsTaskRepeatedly() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        CleanupScheduler scheduler = new CleanupScheduler(FAST, latch::countDown);

        scheduler.start();
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void keepsRunningAfterTaskFailure() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        CleanupScheduler scheduler = new CleanupScheduler(FAST, () -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("тестовая ошибка");
            }
            latch.countDown();
        });

        scheduler.start();
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void stopPreventsFurtherRuns() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CleanupScheduler scheduler = new CleanupScheduler(FAST, () -> {
            calls.incrementAndGet();
            started.countDown();
        });

        scheduler.start();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        scheduler.stop();
        int afterStop = calls.get();
        Thread.sleep(100);

        assertEquals(afterStop, calls.get());
    }

    @Test
    void runsInDaemonThread() throws InterruptedException {
        AtomicBoolean daemon = new AtomicBoolean();
        CountDownLatch latch = new CountDownLatch(1);
        CleanupScheduler scheduler = new CleanupScheduler(FAST, () -> {
            daemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        });

        scheduler.start();
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            scheduler.stop();
        }

        assertTrue(daemon.get());
    }

    @Test
    void startTwiceThrows() {
        CleanupScheduler scheduler = new CleanupScheduler(FAST, () -> { });
        scheduler.start();
        try {
            assertThrows(IllegalStateException.class, scheduler::start);
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void stopWithoutStartIsSafe() {
        CleanupScheduler scheduler = new CleanupScheduler(FAST, () -> { });

        assertDoesNotThrow(scheduler::stop);
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertThrows(IllegalArgumentException.class,
            () -> new CleanupScheduler(Duration.ZERO, () -> { }));
        assertThrows(IllegalArgumentException.class,
            () -> new CleanupScheduler(Duration.ofSeconds(-1), () -> { }));
    }
}
