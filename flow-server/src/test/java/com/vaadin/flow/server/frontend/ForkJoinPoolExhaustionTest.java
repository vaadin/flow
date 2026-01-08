/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.Pair;

/**
 * Regression test for https://github.com/vaadin/flow/issues/22756
 * <p>
 * Tests that {@link FrontendUtils#consumeProcessStreams(Process)} does not
 * depend on {@link ForkJoinPool#commonPool()} and can complete even when the
 * common pool is exhausted.
 */
public class ForkJoinPoolExhaustionTest {

    private final List<CompletableFuture<?>> blockingTasks = new ArrayList<>();

    @After
    public void cleanup() {
        // Cancel all blocking tasks to avoid affecting other tests
        blockingTasks.forEach(f -> f.cancel(true));
        blockingTasks.clear();
    }

    /**
     * Test that consumeProcessStreams can complete even when
     * ForkJoinPool.commonPool() is exhausted by other blocking tasks.
     * <p>
     * This test verifies that consumeProcessStreams uses a dedicated executor
     * (virtual threads) instead of the common pool.
     */
    @Test
    public void consumeProcessStreams_shouldNotBeBlockedByExhaustedCommonPool()
            throws Exception {
        // Step 1: Saturate the ForkJoinPool.commonPool() with blocking tasks
        // We need to submit more blocking tasks than the pool's parallelism
        // to ensure all threads are occupied
        int parallelism = ForkJoinPool.commonPool().getParallelism();
        int numBlockingTasks = parallelism + 2;

        // Use a latch to ensure the pool is saturated. We wait for
        // 'parallelism'
        // tasks to start (the max that can run concurrently). The +2 extra
        // tasks
        // will be queued, ensuring the pool is fully occupied.
        CountDownLatch poolSaturated = new CountDownLatch(parallelism);

        // Submit (parallelism + 2) blocking tasks to ensure the pool is fully
        // saturated. The +2 accounts for potential compensation threads.
        for (int i = 0; i < numBlockingTasks; i++) {
            CompletableFuture<?> blocker = CompletableFuture.runAsync(() -> {
                try {
                    poolSaturated.countDown(); // Signal that this task has
                                               // started
                    // Block for 10 seconds (longer than our test timeout)
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }); // Uses commonPool by default
            blockingTasks.add(blocker);
        }

        // Wait until the pool is saturated (more reliable than fixed sleep)
        Assert.assertTrue("Pool didn't saturate in time",
                poolSaturated.await(5, TimeUnit.SECONDS));

        // Step 2: Start a fast process that outputs immediately and exits
        List<String> cmd = List.of(
                Paths.get(System.getProperty("java.home"), "bin", "java")
                        .toFile().getAbsolutePath(),
                "-cp", System.getProperty("java.class.path"),
                FastTestExecutable.class.getName());

        Process process = new ProcessBuilder(cmd).start();

        // Step 3: Call consumeProcessStreams and wait with a reasonable timeout
        // If the implementation uses commonPool, this will timeout because all
        // threads are blocked.
        // If the implementation uses a dedicated executor, this will complete
        // quickly.
        CompletableFuture<Pair<String, String>> streamsFuture = FrontendUtils
                .consumeProcessStreams(process);

        // Wait for the process to complete (should be nearly instant)
        boolean processCompleted = process.waitFor(2, TimeUnit.SECONDS);
        Assert.assertTrue("Process should complete within 2 seconds",
                processCompleted);

        // Step 4: Try to get the streams with a 2-second timeout
        // This is the key assertion - with the buggy code this will timeout
        try {
            Pair<String, String> streams = streamsFuture.get(2,
                    TimeUnit.SECONDS);

            // Verify the output was captured correctly
            String stdOut = streams.getFirst();
            Assert.assertTrue(
                    "Expected stdout to contain test output, but was: "
                            + stdOut,
                    stdOut.contains("FastTestExecutable completed"));
            String stdErr = streams.getSecond();
            Assert.assertTrue(
                    "Expected stdout to contain test output, but was: "
                            + stdErr,
                    stdErr.contains("FastTestExecutable writing to stderr"));

        } catch (java.util.concurrent.TimeoutException e) {
            Assert.fail("consumeProcessStreams should not be blocked by "
                    + "exhausted ForkJoinPool.commonPool(). "
                    + "This indicates the implementation incorrectly uses "
                    + "the common pool instead of a dedicated executor. "
                    + "See https://github.com/vaadin/flow/issues/22756");
        }
    }

    /**
     * A simple test executable that outputs to stdout and exits immediately.
     * Used to test that consumeProcessStreams can capture output quickly.
     */
    public static class FastTestExecutable {
        public static void main(String... args) {
            System.err.println("FastTestExecutable writing to stderr");
            System.out.println("FastTestExecutable completed");
        }
    }
}
