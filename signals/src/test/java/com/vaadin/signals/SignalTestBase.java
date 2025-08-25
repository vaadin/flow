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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for setting up the environment for testing high-level signal
 * features.
 */
public class SignalTestBase {
    private static final ThreadLocal<Executor> currentResultNotifier = new ThreadLocal<Executor>();
    private static final ThreadLocal<Executor> currentEffectDispatcher = new ThreadLocal<Executor>();
    private static final ThreadLocal<Executor> currentFallbackEffectDispatcher = new ThreadLocal<Executor>();

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

    @AfterEach
    void clear() {
        currentResultNotifier.remove();
        currentEffectDispatcher.remove();
        currentFallbackEffectDispatcher.remove();
        SignalFactory.IN_MEMORY_SHARED.clear();
    }
}
