package com.vaadin.signals.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node.Data;

/**
 * Tracks signal value read operations while a task is run.
 */
public class UsageTracker {
    /**
     * The type of usage. This allows a dependency to be targeted to only a
     * specific field in {@link Data} rather than reacting to any change to that
     * node.
     */
    public enum UsageType {
        /**
         * Usage of the regular node value.
         */
        VALUE(Data::value),

        /**
         * Usage of the map children of a node.
         */
        MAP(Data::mapChildren),

        /**
         * Usage of the list children of a node.
         */
        LIST(Data::listChildren),

        /**
         * Usage of the computed value. Only applicable for computed signals.
         */
        COMPUTED(ComputedSignal::extractValue),

        /**
         * Usage of any kind of data in a signal.
         */
        ALL(Data::lastUpdate);

        private Function<Data, Object> extractor;

        private UsageType(Function<Data, Object> extractor) {
            this.extractor = extractor;
        }

        Object extract(Data node) {
            return extractor.apply(node);
        }
    }

    /**
     * A single instance of accessing the value of a node.
     * 
     * @param tree
     *            the signal tree that the node belongs to, not
     *            <code>null</code>
     * @param nodeId
     *            the id of the node within the tree, not <code>null</code>
     * @param type
     *            the type of usage, not <code>null</code>
     * @param referenceValue
     *            the reference value based on the usage type
     */
    public record NodeUsage(SignalTree tree, Id nodeId, UsageType type,
            Object referenceValue) {

        /**
         * Checks whether the reference value has changed according to the
         * current transaction.
         * 
         * @return <code>true</code> if the value has changed,
         *         <code>false</code> if the value is the same
         */
        public boolean hasChanges() {
            Data node = Transaction.getCurrent().read(tree).data(nodeId)
                    .orElse(null);
            if (node == null) {
                // Node has disappeared so it's certainly changed
                return true;
            }

            return !Objects.equals(type.extract(node), referenceValue);
        }

        /**
         * Checks whether any of the provided usage changes has changes.
         * 
         * @param usages
         *            a collection of node usage instances to check, not
         *            <code>null</code>
         * @return <code>true</code> if any usage has changes,
         *         <code>false</code> if no usage has changes
         */
        public static boolean hasChanges(Collection<NodeUsage> usages) {
            return usages.stream().anyMatch(NodeUsage::hasChanges);
        }
    }

    private static final ThreadLocal<Set<NodeUsage>> currentTracker = new ThreadLocal<>();

    private UsageTracker() {
        // Only static methods
    }

    /**
     * Runs the given task while tracking all cases where a node value is used.
     * The task is run in a read-only transaction.
     * 
     * @param task
     *            the task to run, not <code>null</code>
     * @return a set of node usages, not <code>null</code>
     */
    public static Set<NodeUsage> trackUsage(Runnable task) {
        Set<NodeUsage> tracker = new HashSet<>();

        Set<NodeUsage> previousTracker = currentTracker.get();
        try {
            currentTracker.set(tracker);

            Transaction.runInTransaction(task, Transaction.Type.READ_ONLY);
        } finally {
            currentTracker.set(previousTracker);
        }

        return tracker;
    }

    /**
     * Runs the given supplier without tracking signal usage even if a usage
     * tracker is active.
     * 
     * @param <T>
     *            the supplier type
     * @param task
     *            the supplier task to run, not <code>null</code>
     * @return the value returned from the supplier
     */
    public static <T> T untracked(Supplier<T> task) {
        Set<NodeUsage> previousTracker = currentTracker.get();
        if (previousTracker == null) {
            return task.get();
        }

        try {
            currentTracker.remove();

            // Run without transaction to bypass the read-only restriction
            return Transaction.runWithoutTransaction(task);
        } finally {
            currentTracker.set(previousTracker);
        }
    }

    /**
     * Registers usage of a node value with the current usage tracker, if one is
     * present.
     * 
     * @param tree
     *            the signal tree of the used node, not <code>null</code>
     * @param nodeId
     *            the id of the used node, not <code>null</code>
     * @param type
     *            the usage type, not <code>null</code>
     */
    public static void registerUsage(SignalTree tree, Id nodeId,
            UsageType type) {
        Set<NodeUsage> tracker = currentTracker.get();
        if (tracker != null) {
            Object value = Transaction.getCurrent().read(tree).data(nodeId)
                    .map(type::extract).orElse(null);
            tracker.add(new NodeUsage(tree, nodeId, type, value));
        }
    }

    /**
     * Checks whether a usage tracker is currently active.
     * 
     * @return <code>true</code> if a usage tracker is active
     */
    public static boolean isActive() {
        return currentTracker.get() != null;
    }

}
