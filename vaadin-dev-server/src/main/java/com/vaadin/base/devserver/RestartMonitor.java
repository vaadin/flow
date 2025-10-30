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
package com.vaadin.base.devserver;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors for dev server restarts.
 *
 * Analyzes dev server logs lines to identify restarts.
 *
 * Allows to block dev-server requests processing when a restart is happening
 * and to resume it once the restart is completed.
 */
class RestartMonitor {
    private final Lock lock = new ReentrantLock();
    private final Condition serverRestartedCondition = lock.newCondition();
    private volatile boolean serverRestarting = false;

    private final Pattern restarting;
    private final Pattern restarted;

    /**
     * Creates a new instance of RestartMonitor given the patterns to detect
     * when server initiates a restart and when the restart is completed.
     *
     * @param restarting
     *            a pattern to match with the output to determine that the
     *            server is restarting.
     * @param restarted
     *            a pattern to match with the output to determine that the
     *            server has been restarted.
     */
    RestartMonitor(Pattern restarting, Pattern restarted) {
        this.restarting = restarting;
        this.restarted = restarted;
    }

    void parseLine(String line) {
        if (restarting.matcher(line).find()) {
            serverRestarting();
        } else if (restarted.matcher(line).find()) {
            serverRestarted();
        }
    }

    /**
     * Blocks requests to dev-server during dev-server restarts.
     */
    public void waitForServerReady() {
        lock.lock();
        try {
            while (serverRestarting) {
                String threadName = Thread.currentThread().getName();
                long threadId = Thread.currentThread().getId();
                getLogger().trace(
                        "Thread {} ({}) waiting for dev server restart...",
                        threadName, threadId);
                if (serverRestartedCondition.await(60, TimeUnit.SECONDS)) {
                    getLogger().trace(
                            "Thread {} ({}) continues execution after server restarts",
                            threadName, threadId);
                } else {
                    getLogger().trace(
                            "Thread {} ({}) continues execution after waiting for 60 seconds for a restart to complete",
                            threadName, threadId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void serverRestarting() {
        lock.lock();
        try {
            getLogger().debug("Dev server is restarting...");
            serverRestarting = true;
        } finally {
            lock.unlock();
        }
    }

    private void serverRestarted() {
        lock.lock();
        try {
            serverRestarting = false;
            getLogger().debug("Dev server restarted");
            serverRestartedCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(RestartMonitor.class);
    }
}
