package com.xy.lucky.message.message.dispatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LightweightTimeWheelTest {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
        executor.shutdownNow();
    }

    @Test
    void scheduleShouldRunTaskAfterDelay() throws InterruptedException {
        LightweightTimeWheel timeWheel = new LightweightTimeWheel(20, 64, scheduler, executor);
        timeWheel.start();
        CountDownLatch latch = new CountDownLatch(1);

        timeWheel.schedule(latch::countDown, 80);

        assertThat(latch.await(800, TimeUnit.MILLISECONDS)).isTrue();
        timeWheel.stop();
    }

    @Test
    void scheduleShouldRunAllTasksUnderBurstLoad() throws InterruptedException {
        LightweightTimeWheel timeWheel = new LightweightTimeWheel(10, 128, scheduler, executor);
        timeWheel.start();
        AtomicInteger counter = new AtomicInteger(0);
        int taskSize = 200;
        CountDownLatch latch = new CountDownLatch(taskSize);

        for (int i = 0; i < taskSize; i++) {
            long delay = (i % 20) * 5L;
            timeWheel.schedule(() -> {
                counter.incrementAndGet();
                latch.countDown();
            }, delay);
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(counter.get()).isEqualTo(taskSize);
        timeWheel.stop();
    }

    @Test
    void scheduleShouldFailWhenWheelNotStarted() {
        LightweightTimeWheel timeWheel = new LightweightTimeWheel(20, 64, scheduler, executor);

        assertThatThrownBy(() -> timeWheel.schedule(() -> {
        }, 20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not started");
    }
}
