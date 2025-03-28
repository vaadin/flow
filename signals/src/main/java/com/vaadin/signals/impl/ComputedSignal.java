package com.vaadin.signals.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.vaadin.signals.Id;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.NodeSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.impl.UsageTracker.NodeUsage;
import com.vaadin.signals.impl.UsageTracker.UsageType;

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
    private record ComputedState(Object value, List<NodeUsage> dependencies) {
    }

    private final Supplier<T> computation;

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

    @Override
    protected void registerUsage() {
        if (UsageTracker.isActive()) {
            Optional<Data> node = Transaction.getCurrent().read(tree())
                    .data(id());
            ComputedState state = readState(node);
            if (state != null) {
                /*
                 * Synchronized to avoid memory corruption if invalidation
                 * happens before registration has finished
                 */
                List<Runnable> cleanups = Collections
                        .synchronizedList(new ArrayList<>());

                // Ensure invalidation runs exactly once
                AtomicBoolean invalidated = new AtomicBoolean();

                for (NodeUsage dependency : state.dependencies) {
                    /*
                     * Revalidate if any dependency changes. This will in turn
                     * notify our dependencies.
                     */
                    Runnable cleanup = dependency.tree()
                            .observeNextChange(dependency.nodeId(), () -> {
                                if (invalidated.compareAndSet(false, true)) {
                                    cleanups.forEach(Runnable::run);
                                    getValidState(node);
                                }
                            });
                    cleanups.add(cleanup);

                    if (invalidated.get()) {
                        // Make sure cleanup was done for this one
                        cleanup.run();
                        // Stop registering if already invalidated
                        break;
                    }
                }
            }

            super.registerUsage();
        }
    }

    private ComputedState getValidState(Optional<Data> node) {
        ComputedState state = readState(node);

        if (state == null || NodeUsage.hasChanges(state.dependencies)) {
            Object[] holder = new Object[1];
            Set<NodeUsage> dependencies = UsageTracker
                    .trackUsage(() -> holder[0] = computation.get());
            Object value = holder[0];

            state = new ComputedState(value, List.copyOf(dependencies));

            submit(new SignalCommand.SetCommand(Id.random(), id(),
                    new POJONode(state)));
        }

        return state;
    }

    private static ComputedState readState(Optional<Data> node) {
        return node.map(Data::value).map(ComputedSignal::extractState)
                .orElse(null);
    }

    private static ComputedState extractState(JsonNode json) {
        POJONode pojoNode = (POJONode) json;
        return (ComputedState) pojoNode.getPojo();
    }

    static Object extractValue(Data node) {
        return extractState(node.value()).value;
    }

    @Override
    protected T extractValue(Optional<Data> node) {
        ComputedState state = getValidState(node);

        @SuppressWarnings("unchecked")
        T value = (T) state.value;
        return value;
    }

    @Override
    protected UsageType usageType() {
        return UsageType.COMPUTED;
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
