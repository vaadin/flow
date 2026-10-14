/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RestartMonitorTest {

    private final RestartMonitor monitor = new RestartMonitor(
            Pattern.compile("^restart$"),
            Pattern.compile("^restart(ed| failed)$"));

    private ScheduledExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newScheduledThreadPool(4);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdown();
    }

    @Test
    public void waitForServerReady_serverReady_notBlocking()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(() -> simulateTask(latch));
        Assert.assertTrue(
                "Not restarting, execution should not have been blocked",
                latch.await(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void waitForServerReady_serverRestarting_executionBlockedAndResumed()
            throws InterruptedException {
        simulateServerRestart("restarted");
    }

    @Test
    public void waitForServerReady_serverRestartFailure_executionBlockedAndResumed()
            throws InterruptedException {
        simulateServerRestart("restart failed");
    }

    private void simulateServerRestart(String restartMessage)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        monitor.parseLine("restart");
        executorService.submit(() -> simulateTask(latch));
        executorService.schedule(() -> {
            monitor.parseLine(restartMessage);
            latch.countDown();
        }, 1, TimeUnit.SECONDS);
        Assert.assertEquals("Restarting, execution should be blocked", 2,
                latch.getCount());
        Assert.assertFalse("Restarting, execution should be blocked",
                latch.await(500, TimeUnit.MILLISECONDS));
        Assert.assertTrue(
                "Restart completed, execution should have been completed",
                latch.await(1100, TimeUnit.MILLISECONDS));
    }

    private void simulateTask(CountDownLatch latch) {
        monitor.waitForServerReady();
        latch.countDown();
    }

}
