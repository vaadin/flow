/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartMonitorTest {

    private final RestartMonitor monitor = new RestartMonitor(
            Pattern.compile("^restart$"),
            Pattern.compile("^restart(ed| failed)$"));

    private ScheduledExecutorService executorService;

    @BeforeEach
    void setUp() throws Exception {
        executorService = Executors.newScheduledThreadPool(4);
    }

    @AfterEach
    void tearDown() throws Exception {
        executorService.shutdown();
    }

    @Test
    void waitForServerReady_serverReady_notBlocking()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        executorService.submit(() -> simulateTask(latch));
        assertTrue(latch.await(100, TimeUnit.MILLISECONDS),
                "Not restarting, execution should not have been blocked");
    }

    @Test
    void waitForServerReady_serverRestarting_executionBlockedAndResumed()
            throws InterruptedException {
        simulateServerRestart("restarted");
    }

    @Test
    void waitForServerReady_serverRestartFailure_executionBlockedAndResumed()
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
        assertEquals(2, latch.getCount(),
                "Restarting, execution should be blocked");
        assertFalse(latch.await(500, TimeUnit.MILLISECONDS),
                "Restarting, execution should be blocked");
        assertTrue(latch.await(1100, TimeUnit.MILLISECONDS),
                "Restart completed, execution should have been completed");
    }

    private void simulateTask(CountDownLatch latch) {
        monitor.waitForServerReady();
        latch.countDown();
    }

}
