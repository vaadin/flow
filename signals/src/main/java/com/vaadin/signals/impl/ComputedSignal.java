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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.vaadin.signals.Id;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.NodeSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.impl.UsageTracker.Usage;

/**
 * A signal with a value that is computed based on the value of other signals.
 * The signal value will be lazily re-computed when needed after the value has
 * changed for any of the signals that were used when computing the previous
 * value.
 *
 * @param <T>
 *            the value type
 */
public class ComputedSignal<T> extends Signal<T> {

    /*
     * This state is never supposed to be synchronized across a cluster or to
     * Hilla clients.
     */
    private record ComputedState(Object value, Usage dependencies) {
    }

    private final Supplier<T> computation;

    private int dependentCount = 0;
    private Runnable dependencyRegistration;

    /**
     * Creates a new computed signal with the provided compute callback.
     *
     * @param computation
     *            a callback that returns the computed value, not null
     */
    public ComputedSignal(Supplier<T> computation) {
        super(new SynchronousSignalTree(true), Id.ZERO, ANYTHING_GOES);
        this.computation = Objects.requireNonNull(computation);
    }

    private void revalidateAndListen() {
        if (dependencyRegistration != null) {
            dependencyRegistration.run();
        }
        ComputedState state = getValidState(data(Transaction.getCurrent()));
        dependencyRegistration = state.dependencies.onNextChange(() -> {
            revalidateAndListen();
            return Boolean.FALSE;
        });
    }

    private synchronized void increaseDependents() {
        if (dependentCount++ == 0) {
            revalidateAndListen();
        }
    }

    private synchronized void decreaseDependents() {
        if (--dependentCount == 0) {
            dependencyRegistration.run();
            dependencyRegistration = null;
        }
    }

    @Override
    protected Usage createUsage(Transaction transaction) {
        Usage baseUsage = super.createUsage(transaction);

        return new Usage() {
            @Override
            public boolean hasChanges() {
                return baseUsage.hasChanges();
            }

            @Override
            public Runnable onNextChange(TransientListener listener) {
                increaseDependents();
                AtomicBoolean removed = new AtomicBoolean();

                Runnable cleanup = baseUsage.onNextChange(() -> {
                    boolean keep = listener.invoke();
                    if (!keep && !removed.getAndSet(true)) {
                        decreaseDependents();
                    }
                    return keep;
                });

                return () -> {
                    cleanup.run();
                    if (!removed.getAndSet(true)) {
                        decreaseDependents();
                    }
                };
            }
        };
    }

    private ComputedState getValidState(Data data) {
        ComputedState state = readState(data);

        if (state == null || state.dependencies.hasChanges()) {
            Object[] holder = new Object[1];
            Usage dependencies = UsageTracker
                    .track(() -> holder[0] = computation.get());
            Object value = holder[0];

            state = new ComputedState(value, dependencies);

            submit(new SignalCommand.SetCommand(Id.random(), id(),
                    new POJONode(state)));
        }

        return state;
    }

    private static ComputedState readState(Data data) {
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
        POJONode pojoNode = (POJONode) json;
        return (ComputedState) pojoNode.getPojo();
    }

    @Override
    protected T extractValue(Data data) {
        ComputedState state = getValidState(data);

        @SuppressWarnings("unchecked")
        T value = (T) state.value;
        return value;
    }

    @Override
    protected Object usageChangeValue(Data data) {
        return extractState(data.value()).value;
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
    public NodeSignal asNode() {
        throw new UnsupportedOperationException(
                "Cannot use a computed signal as a node signal");
    }
}
