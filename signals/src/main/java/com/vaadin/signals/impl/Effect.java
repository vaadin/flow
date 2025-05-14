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
 * Applies a side effect based on signal value changes. An effect is a callback
 * that is initially run when the effect is created, and subsequently run again
 * whenever any dependency changes. Dependencies are automatically registered
 * for all signals that are read from the callback. The callback is run again
 * whenever there's a change to any dependency. Dependencies are always updated
 * based the signals read during the most recent invocation.
 */
public class Effect {
    private final Executor dispatcher = SignalEnvironment
            .asynchronousDispatcher();

    private Usage dependencies;
    private Runnable registration;

    // Non-final to allow clearing when the effect is closed
    private Runnable action;

    private final AtomicBoolean invalidateScheduled = new AtomicBoolean(false);

    /**
     * Creates a signal effect with the given action. The action is run when the
     * effect is created and is subsequently run again whenever there's a change
     * to any signal value that was read during the last invocation.
     *
     * @param action
     *            the action to use, not <code>null</code>
     */
    public Effect(Runnable action) {
        assert action != null;
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
     * Disposes this effect by unregistering all current dependencies and
     * preventing the action from running again.
     */
    public synchronized void dispose() {
        clearRegistrations();
        action = null;
        dependencies = null;
    }
}
