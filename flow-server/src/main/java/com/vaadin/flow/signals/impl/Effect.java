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
package com.vaadin.flow.signals.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableExecutor;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.DeniedSignalUsageException;
import com.vaadin.flow.signals.EffectContext;
import com.vaadin.flow.signals.MissingSignalUsageException;
import com.vaadin.flow.signals.SignalEnvironment;
import com.vaadin.flow.signals.function.ContextualEffectAction;
import com.vaadin.flow.signals.function.EffectAction;

/**
 * Applies a side effect based on signal value changes. An effect is a callback
 * that is initially run when the effect is created, and subsequently run again
 * whenever any dependency changes. Dependencies are automatically registered
 * for all signals that are read from the callback. The callback is run again
 * whenever there's a change to any dependency. Dependencies are always updated
 * based the signals read during the most recent invocation.
 */
public class Effect implements Serializable {
    static {
        UsageStatistics.markAsUsed("flow/signal", null);
    }

    private static final ThreadLocal<LinkedList<Effect>> activeEffects = ThreadLocal
            .withInitial(() -> new LinkedList<>());

    private SerializableExecutor dispatcher;
    private final List<Registration> registrations = new ArrayList<>();
    private final List<UsageTracker.Usage> usages = new ArrayList<>();

    // Non-final to allow clearing when the effect is closed
    private @Nullable SerializableRunnable action;

    private final AtomicBoolean invalidateScheduled = new AtomicBoolean(false);

    private @Nullable UI ownerUI;

    private boolean firstRun = true;
    private volatile boolean invalidatedFromBackground = false;
    private boolean passivated = false;

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
    public Effect(EffectAction action) {
        this(action, SignalEnvironment.getDefaultEffectDispatcher()::execute);
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
    public Effect(EffectAction action, SerializableExecutor dispatcher) {
        this((ContextualEffectAction) ctx -> action.execute(), dispatcher);
    }

    /**
     * Creates a context-aware signal effect with the given action and the
     * default dispatcher. The action receives an {@link EffectContext} that
     * provides information about why the effect is running (initial render,
     * user request, or background change).
     *
     * @see SignalEnvironment#getDefaultEffectDispatcher()
     *
     * @param action
     *            the context-aware action to use, not <code>null</code>
     */
    public Effect(ContextualEffectAction action) {
        this(action, SignalEnvironment.getDefaultEffectDispatcher()::execute);
    }

    /**
     * Creates a context-aware signal effect with the given action and a custom
     * dispatcher. The action receives an {@link EffectContext} that provides
     * information about why the effect is running. The dispatcher can be used
     * to make sure changes are evaluated asynchronously or with some specific
     * context available. The action itself needs to be synchronous to be able
     * to track changes.
     *
     * @param action
     *            the context-aware action to use, not <code>null</code>
     * @param dispatcher
     *            the dispatcher to use when handling changes, not
     *            <code>null</code>
     */
    public Effect(ContextualEffectAction action,
            SerializableExecutor dispatcher) {
        assert action != null;
        this.action = () -> {
            try {
                EffectContext ctx = new EffectContext(firstRun,
                        invalidatedFromBackground);
                firstRun = false;
                invalidatedFromBackground = false;
                action.execute(ctx);
            } catch (DeniedSignalUsageException e) {
                // Programming error: signal.get() used in wrong context.
                // Always propagate so the caller gets an immediate
                // exception.
                throw e;
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

        dispatcher.execute(() -> {
            synchronized (this) {
                revalidate();
            }
        });
    }

    private void revalidate() {
        assert registrations.isEmpty();

        if (action == null || passivated) {
            // closed or passivated
            return;
        }

        usages.clear();

        activeEffects.get().add(this);
        try {
            boolean[] hasSignalUsage = { false };
            // Ensure effect runs only once per change event, even if the same
            // signal is read multiple times (each read registers a listener)
            boolean[] changeHandled = { false };
            UsageTracker.track(action, usage -> {
                hasSignalUsage[0] = true;
                usages.add(usage);
                registrations.add(usage
                        .onNextChange(createChangeListener(changeHandled)));
            });
            if (!hasSignalUsage[0]) {
                throw new MissingSignalUsageException(
                        "Effect action must read at least one signal value.");
            }
        } finally {
            Effect removed = activeEffects.get().removeLast();
            assert removed == this;
        }
    }

    private TransientListener createChangeListener(boolean[] changeHandled) {
        // avoid lambda to allow proper deserialization
        return new TransientListener() {
            @Override
            public boolean invoke(boolean immediate) {
                if (!changeHandled[0]) {
                    changeHandled[0] = true;
                    return onDependencyChange(immediate);
                }
                return false;
            }
        };
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

        if (ownerUI != null) {
            invalidatedFromBackground = UI.getCurrent() != ownerUI;
        } else {
            invalidatedFromBackground = VaadinRequest.getCurrent() == null;
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
        registrations.forEach(Registration::remove);
        registrations.clear();
    }

    /**
     * Sets the owner UI for this effect. When set, background change detection
     * compares {@link UI#getCurrent()} against this UI instead of only checking
     * for the presence of a {@link VaadinRequest}. This allows effects to
     * correctly detect changes triggered by another user's session on a shared
     * signal.
     *
     * @param ui
     *            the owner UI, or {@code null} to fall back to
     *            VaadinRequest-based detection
     */
    public void setOwnerUI(@Nullable UI ui) {
        this.ownerUI = ui;
    }

    /**
     * Sets the dispatcher to use for subsequent invalidation callbacks. This
     * can be used to change the execution context before re-activating a
     * passivated effect.
     *
     * @param dispatcher
     *            the new dispatcher to use, not <code>null</code>
     */
    public synchronized void setDispatcher(SerializableExecutor dispatcher) {
        assert dispatcher != null;
        this.dispatcher = dispatcher;
    }

    /**
     * Passivates this effect by removing all dependency listeners while
     * preserving the tracked usages. The effect can later be re-activated with
     * {@link #activate()}, which will check if any tracked values have changed
     * and only re-run the callback if needed.
     */
    public synchronized void passivate() {
        clearRegistrations();
        passivated = true;
    }

    /**
     * Re-activates a previously passivated effect. If any tracked signal has
     * changed since passivation, the effect callback is re-run with
     * {@link EffectContext#isInitialRun()} returning {@code true}. If nothing
     * has changed, the effect simply re-registers its dependency listeners
     * without running the callback.
     */
    public synchronized void activate() {
        if (action == null || !passivated) {
            return;
        }
        passivated = false;
        firstRun = true;

        if (usages.isEmpty()
                || usages.stream().anyMatch(UsageTracker.Usage::hasChanges)) {
            // Something changed while passivated, do a full revalidation
            usages.clear();
            revalidate();
        } else {
            // Nothing changed, just re-register listeners. A change
            // listener may still fire immediately if a change sneaks in
            // between the hasChanges check and the onNextChange call,
            // in which case firstRun is already set to true above.
            // Ensure effect runs only once even if the same signal is
            // registered multiple times
            boolean[] changeHandled = { false };
            for (UsageTracker.Usage usage : usages) {
                registrations.add(usage
                        .onNextChange(createChangeListener(changeHandled)));
            }
            if (!invalidateScheduled.get()) {
                // No invalidation was scheduled, so no change was
                // detected during re-registration. Reset firstRun for
                // normal tracking. If an invalidation was scheduled,
                // firstRun stays true and will be reset by the
                // eventual revalidation.
                firstRun = false;
            }
        }
    }

    /**
     * Disposes this effect by unregistering all current dependencies and
     * preventing the action from running again.
     */
    public synchronized void dispose() {
        clearRegistrations();
        usages.clear();
        action = null;
    }

}
