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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Executor dispatcher;
    private Usage dependencies;
    private Runnable registration;

    // Non-final to allow clearing when the effect is closed
    private Runnable action;

    private final AtomicBoolean invalidateScheduled = new AtomicBoolean(false);

    /**
     * Creates a signal effect with the given action and the default dispatcher.
     * The action is run when the effect is created and is subsequently run
     * again whenever there's a change to any signal value that was read during
     * the last invocation.
     *
     * @see SignalEnvironment#getDefaultEffectDispatcher()
     *
     * @param action
     *            the action to use, not <code>null</code>
     */
    public Effect(Runnable action) {
        this(action, SignalEnvironment.getDefaultEffectDispatcher());
    }

    /**
     * Creates a signal effect with the given action and a custom dispatcher.
     * The action is run when the effect is created and is subsequently run
     * again whenever there's a change to any signal value that was read during
     * the last invocation. The dispatcher can be used to make sure changes are
     * evaluated asynchronously or with some specific context available. The
     * action itself needs to be synchronous to be able to track changes.
     *
     * @param action
     *            the action to use, not <code>null</code>
     * @param dispatcher
     *            the dispatcher to use when handling changes, not
     *            <code>null</code>
     */
    public Effect(Runnable action, Executor dispatcher) {
        assert action != null;
        this.action = () -> {
            try {
                action.run();
            } catch (Exception e) {
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread,
                        e);
            } catch (Error e) {
                getLogger().error(
                        "Uncaught error from effect. The effect will no longer be active.",
                        e);
                dispose();
            }
        };

        assert dispatcher != null;
        this.dispatcher = dispatcher;

        dispatcher.execute(this::revalidate);
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

    private static final Logger getLogger() {
        return LoggerFactory.getLogger(Effect.class.getName());
    }

}
