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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContext;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaadinAsyncStartupTest {

    private final ExecutorService executor = Executors
            .newSingleThreadExecutor();
    private final ServletContext servletContext = new MockServletContext();

    @AfterEach
    void shutDownExecutor() {
        executor.shutdownNow();
    }

    @Test
    void submit_runsOnExecutorWithCallerClassLoader_awaitWaitsForIt()
            throws Exception {
        VaadinAsyncStartup startup = new VaadinAsyncStartup(executor);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> thread = new AtomicReference<>();
        AtomicReference<ClassLoader> classLoader = new AtomicReference<>();
        ClassLoader applicationLoader = new URLClassLoader(new URL[0]);

        Thread caller = Thread.currentThread();
        ClassLoader previous = caller.getContextClassLoader();
        caller.setContextClassLoader(applicationLoader);
        try {
            startup.submit(servletContext, () -> {
                thread.set(Thread.currentThread());
                classLoader.set(Thread.currentThread().getContextClassLoader());
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } finally {
            caller.setContextClassLoader(previous);
        }

        Thread waiter = new Thread(
                () -> VaadinAsyncStartup.await(servletContext));
        waiter.start();
        waiter.join(200);
        assertTrue(waiter.isAlive(),
                "await must block while the initialization runs");

        release.countDown();
        waiter.join(5000);
        assertFalse(waiter.isAlive());
        assertNotSame(caller, thread.get());
        assertSame(applicationLoader, classLoader.get());
    }

    @Test
    void initializationFails_startAndServletWaitThrowTheFailure() {
        VaadinAsyncStartup startup = new VaadinAsyncStartup(executor);
        IllegalStateException failure = new IllegalStateException(
                "route scan failed");

        startup.submit(servletContext, () -> {
            throw failure;
        });

        assertSame(failure,
                assertThrows(IllegalStateException.class, startup::start));
        assertFalse(startup.isRunning());
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> VaadinAsyncStartup.await(servletContext)));
    }

    @Test
    void nothingSubmitted_startReturnsAtOnce() {
        VaadinAsyncStartup startup = new VaadinAsyncStartup(executor);

        startup.start();

        assertTrue(startup.isRunning());
    }
}
