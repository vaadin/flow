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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for setting up the environment for testing high-level signal
 * features.
 */
public class SignalTestBase {
    private static final ThreadLocal<Executor> currentDispatcher = new ThreadLocal<Executor>();
    private static final ThreadLocal<Executor> currentOverrideDispatcher = new ThreadLocal<Executor>();

    static final Executor dispatcher = task -> {
        Executor executor = currentDispatcher.get();
        if (executor != null) {
            executor.execute(task);
        } else {
            /*
             * The specification requires that this dispatcher is asynchronous
             * to prevent blocking a signal value setter until all effects have
             * been invoked. We're still using direct synchronous dispatching in
             * tests to avoid making most tests more complex than necessary.
             */
            task.run();
        }
    };

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

    @BeforeAll
    static void setupEnvironment() {
        if (SignalEnvironment.tryInitialize(new ObjectMapper(), dispatcher)) {
            SignalEnvironment
                    .addDispatcherOverride(currentOverrideDispatcher::get);
        } else {
            assert SignalEnvironment.defaultDispatcher() == dispatcher;
        }
    }

    protected TestExecutor useTestOverrideDispatcher() {
        TestExecutor dispatcher = new TestExecutor();

        currentOverrideDispatcher.set(dispatcher);

        return dispatcher;
    }

    protected TestExecutor useTestDispatcher() {
        TestExecutor dispatcher = new TestExecutor();

        currentDispatcher.set(dispatcher);

        return dispatcher;
    }

    @AfterEach
    void clear() {
        currentOverrideDispatcher.remove();
        currentDispatcher.remove();
        SignalFactory.IN_MEMORY_SHARED.clear();
    }
}
