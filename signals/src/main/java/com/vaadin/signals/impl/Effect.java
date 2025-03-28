package com.vaadin.signals.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.signals.SignalEnvironment;
import com.vaadin.signals.impl.UsageTracker.NodeUsage;

/**
 * Applies a side effect based on signal value changes. An effect is based on a
 * callback that is initially run when the effect is created and then it's run.
 * Dependencies are automatically registered for all signals that are read from
 * the callback. The callback is run again whenever the value of any of those
 * signals change. Dependencies are always updated based the signals read during
 * the most recent invocation.
 */
public class Effect {
    private final List<Runnable> registrations = new ArrayList<>();
    private final Executor dispatcher = SignalEnvironment
            .asynchronousDispatcher();

    private Set<NodeUsage> dependencies = Set.of();

    // Non-final to allow clearing when the effect is closed
    private Runnable action;

    private final AtomicBoolean invalidateScheduled = new AtomicBoolean(false);

    public Effect(Runnable action) {
        this.action = action;

        revalidate();
    }

    private void revalidate() {
        assert registrations.isEmpty();

        if (action == null) {
            // closed
            return;
        }

        dependencies = UsageTracker.trackUsage(action);

        for (NodeUsage dependency : dependencies) {
            Runnable registration = dependency.tree().observeNextChange(
                    dependency.nodeId(), this::scheduleInvalidate);
            registrations.add(registration);
        }
    }

    private void scheduleInvalidate() {
        if (invalidateScheduled.compareAndSet(false, true)) {
            dispatcher.execute(this::invalidate);
        }
    }

    private synchronized void invalidate() {
        invalidateScheduled.set(false);

        if (!NodeUsage.hasChanges(dependencies)) {
            return;
        }

        clearRegistrations();

        revalidate();
    }

    private void clearRegistrations() {
        registrations.forEach(Runnable::run);
        registrations.clear();
    }

    /**
     * Close this effect to unregister all current dependencies and prevent the
     * action from running again.
     */
    public synchronized void close() {
        clearRegistrations();
        action = null;
        dependencies = Set.of();
    }
}
