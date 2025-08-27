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
package com.vaadin.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class SignalEnvironmentTest extends SignalTestBase {

    @Test
    void registerAndUnregister_environmentIsUsedUntilUnregistered() {
        Thread testThread = Thread.currentThread();
        AtomicInteger count = new AtomicInteger();

        SignalEnvironment environment = new SignalEnvironment() {
            @Override
            public boolean isActive() {
                return Thread.currentThread() == testThread;
            }

            @Override
            public Executor getResultNotifier() {
                count.incrementAndGet();
                return null;
            }

            @Override
            public Executor getFallbackEffectDispatcher() {
                return null;
            }

            @Override
            public Executor getEffectDispatcher() {
                return null;
            }
        };

        SignalEnvironment.getCurrentResultNotifier();
        assertEquals(0, count.get());

        Runnable unregister = SignalEnvironment.register(environment);
        try {
            SignalEnvironment.getCurrentResultNotifier();
            assertEquals(1, count.get());
        } finally {
            unregister.run();
        }

        SignalEnvironment.getCurrentResultNotifier();
        assertEquals(1, count.get());
    }

    @Test
    void isActive_environmentUsedOnlyWhenActive() {
        ThreadLocal<Boolean> active = new ThreadLocal<Boolean>();
        AtomicInteger count = new AtomicInteger();

        SignalEnvironment environment = new SignalEnvironment() {
            @Override
            public boolean isActive() {
                return active.get() == Boolean.TRUE;
            }

            @Override
            public Executor getResultNotifier() {
                count.incrementAndGet();
                return null;
            }

            @Override
            public Executor getFallbackEffectDispatcher() {
                return null;
            }

            @Override
            public Executor getEffectDispatcher() {
                return null;
            }
        };

        Runnable unregister = SignalEnvironment.register(environment);
        try {
            SignalEnvironment.getCurrentResultNotifier();
            assertEquals(0, count.get());

            active.set(Boolean.TRUE);
            SignalEnvironment.getCurrentResultNotifier();
            assertEquals(1, count.get());
        } finally {
            unregister.run();
        }
    }

    @Test
    void resultNotifier_noNotifier_runsImmediately() {
        AtomicInteger count = new AtomicInteger();

        SignalEnvironment.getCurrentResultNotifier()
                .execute(() -> count.incrementAndGet());

        assertEquals(1, count.get());
    }

    @Test
    void resultNotifier_notifierDefined_runsThroughDefinedNotifier() {
        AtomicInteger count = new AtomicInteger();

        TestExecutor notifier = useTestResultNotifier();

        SignalEnvironment.getCurrentResultNotifier()
                .execute(() -> count.incrementAndGet());
        assertEquals(0, count.get());

        notifier.runPendingTasks();
        assertEquals(1, count.get());
    }

    @Test
    void effectDispatcher_noDispathcer_runsImmediately() {
        AtomicInteger count = new AtomicInteger();

        SignalEnvironment.getCurrentEffectDispatcher()
                .execute(() -> count.incrementAndGet());

        assertEquals(1, count.get());
    }

    @Test
    void effectDispatcher_setWhenAccessed_usedAfterCleared() {
        AtomicInteger count = new AtomicInteger();

        TestExecutor effectDispatcher = useTestEffectDispatcher();
        TestExecutor fallbackEffectDispatcher = useTestFallbackEffectDispatcher();

        Executor dispatcher = SignalEnvironment.getCurrentEffectDispatcher();
        clearTestEffectDispatcher();

        dispatcher.execute(() -> count.incrementAndGet());
        assertEquals(0, count.get());

        effectDispatcher.runPendingTasks();
        assertEquals(1, count.get());

        assertEquals(0, fallbackEffectDispatcher.countPendingTasks());
    }

    @Test
    void fallbackEffectDispatcher_setWhenDispatching_runThroughDispatcher() {
        AtomicInteger count = new AtomicInteger();

        Executor dispatcher = SignalEnvironment.getCurrentEffectDispatcher();

        TestExecutor effectDispatcher = useTestEffectDispatcher();
        TestExecutor fallbackEffectDispatcher = useTestFallbackEffectDispatcher();

        dispatcher.execute(() -> count.incrementAndGet());
        assertEquals(0, count.get());

        fallbackEffectDispatcher.runPendingTasks();
        assertEquals(1, count.get());

        assertEquals(0, effectDispatcher.countPendingTasks());
    }
}
