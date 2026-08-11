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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.POJONode;

import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.MissingSignalUsageException;
import com.vaadin.flow.signals.Node.Data;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.shared.AbstractSharedSignal;
import com.vaadin.flow.signals.shared.SharedNodeSignal;
import com.vaadin.flow.signals.shared.impl.SynchronousSignalTree;

/**
 * A signal that caches the value of an inner signal. The inner signal is
 * typically a computed signal performing an expensive computation that should
 * be run again only if any of its dependencies has changed.
 * <p>
 * If a {@link RuntimeException} is thrown when getting the value of the inner
 * signal, then that exception will be re-thrown when accessing the value of
 * this signal. An {@link Signal#unboundEffect(EffectAction) effect} or outer
 * cached signal that uses the value from a cached signal will not be
 * invalidated if the computation is run again but produces the same value as
 * before.
 *
 * @param <T>
 *            the value type
 */
public class CachedSignal<T extends @Nullable Object>
        extends AbstractSharedSignal<T> {

    /*
     * This state is never supposed to be synchronized across a cluster or to
     * Hilla clients.
     */
    private record CacheState(@Nullable Object value,
            @Nullable RuntimeException exception,
            Usage dependencies) implements Serializable {
    }

    private final Signal<T> inner;

    private int dependentCount = 0;
    private @Nullable Registration dependencyRegistration;

    /*
     * Identifies the latest attempt to (re)listen to the dependencies. Every
     * revalidation and every teardown of the last external listener bumps it
     * while holding the monitor. Because dependency listeners are
     * (un)registered without the monitor held -- to avoid an ABBA deadlock
     * between this signal's monitor and a dependency's SignalTree lock (see
     * #25166) -- several attempts can run concurrently; only the one whose
     * captured generation still matches is allowed to install its registration,
     * so the most recent attempt always wins and stale attempts remove their
     * registration instead of leaking it.
     */
    private long generation = 0;

    /**
     * Creates a new cached signal with the provided inner signal.
     *
     * @param inner
     *            a signal to wrap, not null
     */
    public CachedSignal(Signal<T> inner) {
        super(new SynchronousSignalTree(true), Id.ZERO, ANYTHING_GOES);
        this.inner = Objects.requireNonNull(inner);
    }

    /**
     * As long as nobody listens to changes to this cached signal, we can just
     * re-validate on demand every time the value is read. But when there's at
     * least one active listener, we need to actively listen to changes in our
     * dependencies.
     * <p>
     * Whenever a dependency changes, we run {@link #getValidState(Data)}. This
     * causes the inner signal to be read again. If that leads to a new value in
     * the tree, then the {@link Usage} from the super class will be triggered
     * which in notifies out external listeners.
     * <p>
     * We have just a single set of internal listeners on our dependencies even
     * if there are multiple external listeners so that the inner signal is read
     * only once. We keep track of how many active external listeners we have so
     * that our internal listener is active if and only if there's at least one
     * active external listener.
     */
    private void revalidateAndListen() {
        Registration staleRegistration;
        long myGeneration;
        synchronized (this) {
            if (dependentCount == 0) {
                // Nobody is listening anymore; the un-count path owns teardown.
                return;
            }
            // This attempt becomes the latest one; any concurrent attempt that
            // captured an earlier generation discards its registration.
            myGeneration = ++generation;
            // Capture and clear listeners on old dependencies, but remove them
            // outside the monitor (see below).
            staleRegistration = dependencyRegistration;
            dependencyRegistration = null;
        }

        // Clear listeners on old dependencies. Removal acquires the
        // dependency's SignalTree lock, so it must run without the monitor held
        // to avoid an ABBA deadlock with a thread committing a change to a
        // dependency while holding that lock (see #25166).
        if (staleRegistration != null) {
            staleRegistration.remove();
        }

        // Read inner value to get new dependencies. Reading dependencies (and
        // possibly submitting the refreshed cache value) acquires tree locks,
        // so this must likewise run without the monitor held.
        CacheState state = getValidState(data(Transaction.getCurrent()));

        // avoid lambda to allow proper deserialization
        TransientListener usageListener = new TransientListener() {
            @Override
            public boolean invoke(boolean immediate) {
                revalidateAndListen();
                return false;
            }
        };
        // Listen to the new dependencies. onNextChange also acquires the
        // dependency tree locks and thus runs without the monitor held.
        Registration newRegistration = state.dependencies
                .onNextChange(usageListener);

        boolean superseded;
        synchronized (this) {
            // Discard if nobody is listening anymore or a newer attempt
            // superseded this one. Removing the listener we just registered
            // avoids leaking it on the dependency trees.
            superseded = dependentCount == 0 || generation != myGeneration;
            if (!superseded) {
                dependencyRegistration = newRegistration;
            }
        }
        if (superseded) {
            newRegistration.remove();
        }
    }

    /**
     * Increase the number of active external listeners and start listening to
     * our dependencies if previously had no external listener.
     *
     * @return a callback to count back down again, not <code>null</code>
     */
    private Registration countActiveExternalListener() {
        boolean startListening;
        synchronized (this) {
            startListening = dependentCount++ == 0;
        }
        // Start listening outside the monitor: revalidateAndListen acquires
        // dependency tree locks (see #25166).
        if (startListening) {
            revalidateAndListen();
        }

        // Avoid counting down multiple times
        AtomicBoolean removed = new AtomicBoolean();

        return () -> {
            if (!removed.getAndSet(true)) {
                /*
                 * Decrease the number of active external listeners and stop
                 * listening to our dependencies if there are no more external
                 * listeners. The registration is captured under the monitor but
                 * removed outside it, since removal acquires the dependency's
                 * SignalTree lock (see #25166).
                 */
                Registration toRemove = null;
                synchronized (CachedSignal.this) {
                    if (--dependentCount == 0) {
                        // Supersede any revalidation in flight so it discards
                        // its registration instead of installing it.
                        ++generation;
                        toRemove = dependencyRegistration;
                        dependencyRegistration = null;
                    }
                }
                if (toRemove != null) {
                    toRemove.remove();
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * The usage instance from the super implementation is wrapped to count how
     * many active external listeners there are. The external listener might
     * become inactive either by returning <code>false</code> or through
     * explicit unregistering, so both those sources are wrapped to accurately
     * keep track of when the listener is no longer active.
     */
    @Override
    protected Usage createUsage(Transaction transaction) {
        Usage superUsage = super.createUsage(transaction);

        return new Usage() {
            @Override
            public boolean hasChanges() {
                return superUsage.hasChanges();
            }

            @Override
            public Registration onNextChange(TransientListener listener) {
                Registration uncount = countActiveExternalListener();

                // avoid lambda to allow proper deserialization
                TransientListener usageListener = new TransientListener() {
                    @Override
                    public boolean invoke(boolean immediate) {
                        boolean listenToNext = listener.invoke(immediate);
                        if (!listenToNext) {
                            uncount.remove();
                        }
                        return listenToNext;
                    }
                };

                Registration superCleanup = superUsage
                        .onNextChange(usageListener);

                return () -> {
                    superCleanup.remove();
                    uncount.remove();
                };
            }
        };
    }

    private CacheState getValidState(@Nullable Data data) {
        CacheState state = readState(data);

        if (state == null || state.dependencies.hasChanges()) {
            @Nullable
            Object[] holder = new @Nullable Object[2];
            Usage dependencies = UsageTracker.track(() -> {
                try {
                    holder[0] = inner.get();
                } catch (RuntimeException e) {
                    holder[1] = e;
                }
            });
            if (dependencies == UsageTracker.NO_USAGE) {
                throw new MissingSignalUsageException(
                        "A computing inner signal must read at least one other signal value.");
            }
            @Nullable
            Object value = holder[0];
            @Nullable
            RuntimeException exception = (RuntimeException) holder[1];

            state = new CacheState(value, exception, dependencies);

            submit(new SignalCommand.SetCommand(Id.random(), id(),
                    new CachedPOJONode(state)));
        }

        return state;
    }

    private static @Nullable CacheState readState(@Nullable Data data) {
        if (data == null) {
            return null;
        }

        JsonNode value = data.value();
        if (value == null) {
            return null;
        }

        return extractState(value);
    }

    private static CacheState extractState(JsonNode json) {
        CachedPOJONode pojoNode = (CachedPOJONode) json;
        return (CacheState) pojoNode.getPojo();
    }

    private static class CachedPOJONode extends POJONode
            implements Serializable {

        public CachedPOJONode(Object v) {
            super(v);
        }
    }

    @Override
    protected @Nullable T extractValue(@Nullable Data data) {
        CacheState state = getValidState(data);

        if (state.exception != null) {
            throw state.exception;
        }

        @SuppressWarnings("unchecked")
        T value = (T) state.value;
        return value;
    }

    @Override
    protected @Nullable Object usageChangeValue(Data data) {
        JsonNode value = data.value();
        if (value == null) {
            return null;
        }
        return extractState(value).value;
    }

    @Override
    public T peekConfirmed() {
        throw new UnsupportedOperationException("Cannot peek a cached signal");
    }

    @Override
    public SharedNodeSignal asNode() {
        throw new UnsupportedOperationException(
                "Cannot use a cached signal as a node signal");
    }
}
