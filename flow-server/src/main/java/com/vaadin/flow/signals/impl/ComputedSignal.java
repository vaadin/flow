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

import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Node.Data;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.function.CleanupCallback;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.function.SignalComputation;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;
import com.vaadin.flow.signals.shared.AbstractSignal;
import com.vaadin.flow.signals.shared.SharedNodeSignal;
import com.vaadin.flow.signals.shared.impl.SynchronousSignalTree;

/**
 * A signal with a value that is computed based on the value of other signals.
 * The signal value will be lazily re-computed when needed after the value has
 * changed for any of the signals that were used when computing the previous
 * value. If the computation callback throws a {@link RuntimeException}, then
 * that exception will be re-thrown when accessing the value of this signal. An
 * {@link Signal#effect(EffectAction) effect} or computed signal that uses the
 * value from a computed signal will not be invalidated if the computation is
 * run again but produces the same value as before.
 *
 * @param <T>
 *            the value type
 */
public class ComputedSignal<T> extends AbstractSignal<T> {

    /*
     * This state is never supposed to be synchronized across a cluster or to
     * Hilla clients.
     */
    private record ComputedState(@Nullable Object value,
            @Nullable RuntimeException exception,
            Usage dependencies) implements Serializable {
    }

    private final SignalComputation<T> computation;

    private int dependentCount = 0;
    private @Nullable CleanupCallback dependencyRegistration;

    /**
     * Creates a new computed signal with the provided compute callback.
     *
     * @param computation
     *            a callback that returns the computed value, not null
     */
    public ComputedSignal(SignalComputation<T> computation) {
        super(new SynchronousSignalTree(true), Id.ZERO, ANYTHING_GOES);
        this.computation = Objects.requireNonNull(computation);
    }

    /**
     * As long as nobody listens to changes to this computed signal, we can just
     * re-compute on demand every time the value is read. But when there's at
     * least one active listener, we need to actively listen to changes in our
     * dependencies.
     * <p>
     * Whenever a dependency changes, we run {@link #getValidState(Data)}. This
     * causes the compute callback to run again. If that leads to a new value in
     * the tree, then the {@link Usage} from the super class will be triggered
     * which in notifies out external listeners.
     * <p>
     * We have just a single set of internal listeners on our dependencies even
     * if there are multiple external listeners so that the compute callback is
     * run only once. We keep track of how many active external listeners we
     * have so that our internal listener is active if and only if there's at
     * least one active external listener.
     */
    private synchronized void revalidateAndListen() {
        // Clear listeners on old dependencies
        if (dependencyRegistration != null) {
            dependencyRegistration.cleanup();
        }

        // Run compute callback to get new dependencies
        ComputedState state = getValidState(data(Transaction.getCurrent()));

        // avoid lambda to allow proper deserialization
        TransientListener usageListener = new TransientListener() {
            @Override
            public boolean invoke(boolean immediate) {
                revalidateAndListen();
                return false;
            }
        };
        // Listen to the new dependencies
        dependencyRegistration = state.dependencies.onNextChange(usageListener);
    }

    /**
     * Increase the number of active external listeners and start listening to
     * our dependencies if previously had no external listener.
     *
     * @return a callback to count back down again, not <code>null</code>
     */
    private synchronized CleanupCallback countActiveExternalListener() {
        if (dependentCount++ == 0) {
            revalidateAndListen();
        }

        // Avoid counting down multiple times
        AtomicBoolean removed = new AtomicBoolean();

        return () -> {
            if (!removed.getAndSet(true)) {
                synchronized (ComputedSignal.this) {
                    /*
                     * Decrease the number of active external listeners and stop
                     * listening to our dependencies if there are no more
                     * external listeners.
                     */
                    if (--dependentCount == 0) {
                        if (dependencyRegistration != null) {
                            dependencyRegistration.cleanup();
                            dependencyRegistration = null;
                        }
                    }
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
            public CleanupCallback onNextChange(TransientListener listener) {
                CleanupCallback uncount = countActiveExternalListener();

                // avoid lambda to allow proper deserialization
                TransientListener usageListener = new TransientListener() {
                    @Override
                    public boolean invoke(boolean immediate) {
                        boolean listenToNext = listener.invoke(immediate);
                        if (!listenToNext) {
                            uncount.cleanup();
                        }
                        return listenToNext;
                    }
                };

                CleanupCallback superCleanup = superUsage
                        .onNextChange(usageListener);

                return () -> {
                    superCleanup.cleanup();
                    uncount.cleanup();
                };
            }
        };
    }

    private ComputedState getValidState(@Nullable Data data) {
        ComputedState state = readState(data);

        if (state == null || state.dependencies.hasChanges()) {
            @Nullable Object[] holder = new @Nullable Object[2];
            Usage dependencies = UsageTracker.track(() -> {
                try {
                    holder[0] = computation.compute();
                } catch (RuntimeException e) {
                    holder[1] = e;
                }
            });
            @Nullable Object value = holder[0];
            @Nullable RuntimeException exception = (RuntimeException) holder[1];

            state = new ComputedState(value, exception, dependencies);

            submit(new SignalCommand.SetCommand(Id.random(), id(),
                    new ComputedPOJONode(state)));
        }

        return state;
    }

    private static @Nullable ComputedState readState(@Nullable Data data) {
        if (data == null) {
            return null;
        }

        JsonNode value = data.value();
        if (value == null) {
            return null;
        }

        return extractState(value);
    }

    private static ComputedState extractState(JsonNode json) {
        ComputedPOJONode pojoNode = (ComputedPOJONode) json;
        return (ComputedState) pojoNode.getPojo();
    }

    private static class ComputedPOJONode extends POJONode
            implements Serializable {

        public ComputedPOJONode(Object v) {
            super(v);
        }
    }

    @Override
    protected @Nullable T extractValue(@Nullable Data data) {
        ComputedState state = getValidState(data);

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
        throw new UnsupportedOperationException(
                "Cannot peek a computed signal");
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException(
                "Cannot peek a computed signal");
    }

    @Override
    public SharedNodeSignal asNode() {
        throw new UnsupportedOperationException(
                "Cannot use a computed signal as a node signal");
    }
}
