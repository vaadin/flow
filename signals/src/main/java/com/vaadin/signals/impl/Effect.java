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
package com.vaadin.signals.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.signals.SignalEnvironment;
import com.vaadin.signals.impl.UsageTracker.Usage;

/**
 * Applies a side effect based on signal value changes. An effect is based on a
 * callback that is initially run when the effect is created and then it's run.
 * Dependencies are automatically registered for all signals that are read from
 * the callback. The callback is run again whenever the value of any of those
 * signals change. Dependencies are always updated based the signals read during
 * the most recent invocation.
 */
public class Effect {
    private final Executor dispatcher = SignalEnvironment
            .asynchronousDispatcher();

    private Usage dependencies;
    private Runnable registration;

    // Non-final to allow clearing when the effect is closed
    private Runnable action;

    private final AtomicBoolean invalidateScheduled = new AtomicBoolean(false);

    public Effect(Runnable action) {
        this.action = action;

        revalidate();
    }

    private void revalidate() {
        assert registration == null;

        if (action == null) {
            // closed
            return;
        }

        dependencies = UsageTracker.track(action);
        registration = dependencies.onNextChange(() -> {
            scheduleInvalidate();
            return false;
        });
    }

    private void scheduleInvalidate() {
        if (invalidateScheduled.compareAndSet(false, true)) {
            dispatcher.execute(this::invalidate);
        }
    }

    private synchronized void invalidate() {
        invalidateScheduled.set(false);

        clearRegistrations();

        revalidate();
    }

    private void clearRegistrations() {
        if (registration != null) {
            registration.run();
            registration = null;
        }
    }

    /**
     * Close this effect to unregister all current dependencies and prevent the
     * action from running again.
     */
    public synchronized void close() {
        clearRegistrations();
        action = null;
        dependencies = null;
    }
}
