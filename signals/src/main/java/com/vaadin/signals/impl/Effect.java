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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.signals.SignalEnvironment;

/**
 * Applies a side effect based on signal value changes. An effect is a callback
 * that is initially run when the effect is created, and subsequently run again
 * whenever any dependency changes. Dependencies are automatically registered
 * for all signals that are read from the callback. The callback is run again
 * whenever there's a change to any dependency. Dependencies are always updated
 * based the signals read during the most recent invocation.
 */
public class Effect {
    private static final ThreadLocal<LinkedList<Effect>> activeEffects = ThreadLocal
            .withInitial(() -> new LinkedList<>());

    private final Executor dispatcher;
    private final List<Runnable> registrations = new ArrayList<>();

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
                Thread thread = Thread.currentThread();
                thread.getUncaughtExceptionHandler().uncaughtException(thread,
                        new Error(
                                "Uncaught error from effect. The effect will no longer be active.",
                                e));
                dispose();
            }
        };

        assert dispatcher != null;
        this.dispatcher = dispatcher;

        dispatcher.execute(this::revalidate);
    }

    private void revalidate() {
        assert registrations.isEmpty();

        if (action == null) {
            // closed
            return;
        }

        activeEffects.get().add(this);
        try {
            UsageTracker.track(action, usage -> {
                registrations.add(usage.onNextChange(this::onDependencyChange));
            });
        } finally {
            Effect removed = activeEffects.get().removeLast();
            assert removed == this;
        }
    }

    private boolean onDependencyChange(boolean immediate) {
        /*
         * Detect loops by checking if the same effect is already active on this
         * thread. Don't check for immediate updates since an immediate update
         * doesn't run on the same thread that caused the change.
         */
        if (!immediate && activeEffects.get().contains(this)) {
            dispose();
            throw new IllegalStateException(
                    "Infinite loop detected between effect updates. This effect is deactivated.");
        }

        scheduleInvalidate();
        return false;
    }

    private void scheduleInvalidate() {
        if (invalidateScheduled.compareAndSet(false, true)) {
            var inheritedActive = new LinkedList<>(activeEffects.get());

            dispatcher.execute(() -> {
                var oldActive = activeEffects.get();
                activeEffects.set(inheritedActive);

                try {
                    invalidate();
                } finally {
                    activeEffects.set(oldActive);
                }
            });
        }
    }

    private synchronized void invalidate() {
        invalidateScheduled.set(false);

        clearRegistrations();

        revalidate();
    }

    private void clearRegistrations() {
        registrations.forEach(Runnable::run);
        registrations.clear();
    }

    /**
     * Disposes this effect by unregistering all current dependencies and
     * preventing the action from running again.
     */
    public synchronized void dispose() {
        clearRegistrations();
        action = null;
    }

}
