/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.signals;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for setting up the environment for testing high-level signal
 * features.
 */
public class SignalTestBase {
    private static final ThreadLocal<Executor> currentResultNotifier = new ThreadLocal<Executor>();
    private static final ThreadLocal<Executor> currentEffectDispatcher = new ThreadLocal<Executor>();
    private static final ThreadLocal<Executor> currentFallbackEffectDispatcher = new ThreadLocal<Executor>();

    private final List<Throwable> uncaughtExceptions = new ArrayList<>();

    protected class TestExecutor implements Executor {
        private final ArrayList<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        public int countPendingTasks() {
            return tasks.size();
        }

        public int runPendingTasks() {
            List<Runnable> pending = List.copyOf(tasks);
            tasks.clear();

            pending.forEach(Runnable::run);
            return pending.size();
        }
    }

    private static Runnable environmentRegistration;

    @BeforeAll
    static void setupEnvironment() {
        environmentRegistration = SignalEnvironment
                .register(new SignalEnvironment() {
                    @Override
                    public boolean isActive() {
                        return true;
                    }

                    @Override
                    public Executor getResultNotifier() {
                        return currentResultNotifier.get();
                    }

                    @Override
                    public Executor getEffectDispatcher() {
                        return currentEffectDispatcher.get();
                    }

                    @Override
                    public Executor getFallbackEffectDispatcher() {
                        return currentFallbackEffectDispatcher.get();
                    }
                });
    }

    @AfterAll
    static void closeEnvironment() {
        environmentRegistration.run();
    }

    protected TestExecutor useTestResultNotifier() {
        TestExecutor dispatcher = new TestExecutor();

        currentResultNotifier.set(dispatcher);

        return dispatcher;
    }

    protected TestExecutor useTestEffectDispatcher() {
        TestExecutor dispatcher = new TestExecutor();

        currentEffectDispatcher.set(dispatcher);

        return dispatcher;
    }

    protected void clearTestEffectDispatcher() {
        currentEffectDispatcher.remove();
    }

    protected TestExecutor useTestFallbackEffectDispatcher() {
        TestExecutor dispatcher = new TestExecutor();

        currentFallbackEffectDispatcher.set(dispatcher);

        return dispatcher;
    }

    protected void assertUncaughtException(Throwable exception) {
        assertUncaughtException(lastCaught -> lastCaught == exception);
    }

    protected void assertUncaughtException(Predicate<Throwable> predicate) {
        assertFalse(uncaughtExceptions.isEmpty());

        int lastIndex = uncaughtExceptions.size() - 1;
        Throwable lastUncaught = uncaughtExceptions.get(lastIndex);
        if (predicate.test(lastUncaught)) {
            uncaughtExceptions.remove(lastIndex);
        } else {
            fail("Last uncaught exception did not pass test: " + lastUncaught);
        }
    }

    @BeforeEach
    void setupExceptionHandler() {
        Thread currentThread = Thread.currentThread();
        assertSame(
                "Adjustments are needed if a non-standard exception handler is present",
                currentThread.getUncaughtExceptionHandler(),
                currentThread.getThreadGroup());

        currentThread.setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable.getCause() instanceof AssertionError ae) {
                // Fail the test immediately for things asserted by the test
                throw ae;
            } else {
                uncaughtExceptions.add(throwable);
            }
        });
    }

    @AfterEach
    void clear() {
        Thread.currentThread().setUncaughtExceptionHandler(null);

        assertEquals(List.of(), uncaughtExceptions,
                "Exceptions passed to the uncaught exception handler have not been asserted");

        currentResultNotifier.remove();
        currentEffectDispatcher.remove();
        currentFallbackEffectDispatcher.remove();
        SignalFactory.IN_MEMORY_SHARED.clear();
    }
}
