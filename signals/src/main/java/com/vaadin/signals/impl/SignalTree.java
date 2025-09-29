package com.vaadin.signals.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;

/**
 * Provides thread-safe access to a tree of signal nodes and a way of listening
 * for changes to those nodes. There are two primary types of signal trees:
 * synchronous trees have their changes applied immediately whereas asynchronous
 * trees make a differences between submitted changes and changes that have been
 * asynchronously confirmed.
 */
public abstract class SignalTree {
    /**
     * Collection of callbacks representing the possible stages when committing
     * a transaction. The commit is split up into stages to enable a coordinated
     * transaction that includes multiple signal trees.
     *
     * @see SignalTree#prepareCommit(CommandsAndHandlers)
     */
    public interface PendingCommit {
        /**
         * Checks whether the pending changes can be committed. Committing is
         * possible if all changes would be accepted based on the current tree
         * state.
         *
         * @return <code>true</code> if the changes can be committed.
         */
        boolean canCommit();

        /**
         * Updates the tree state so that all pending changes are considered to
         * be submitted.
         */
        void applyChanges();

        /**
         * Sets the result of all pending changes as rejected.
         */
        void markAsAborted();

        /**
         * Notifies dependents and updates all result listeners based on the
         * pending changes.
         */
        void publishChanges();
    }

    /**
     * The tree type, used to determine how different tree instances can be
     * combined in a transaction.
     */
    public enum Type {
        /**
         * Asynchronous trees can only confirm the status of applied commands
         * asynchronously and can thus not participate in transactions that
         * contain other asynchronous or synchronous trees.
         */
        ASYNCHRONOUS,

        /**
         * Computed trees cannot cause conflicts and can thus participate in any
         * transaction without restrictions.
         */
        COMPUTED,

        /**
         * Synchronous trees can confirm the status of applied commands while
         * the tree is locked which makes it possible for multiple sync trees to
         * participate in the same transaction.
         */
        SYNCHRONOUS;
    }

    private final Map<Id, List<TransientListener>> observers = new HashMap<>();

    private final Id id = Id.random();

    private final ReentrantLock lock = new ReentrantLock();

    private final Type type;

    private final List<BiConsumer<SignalCommand, CommandResult>> subscribers = new ArrayList<>();

    /**
     * Creates a new signal tree with the given type.
     *
     * @param type
     *            the signal tree type, not <code>null</code>
     */
    protected SignalTree(Type type) {
        assert type != null;

        this.type = type;
    }

    /**
     * Gets the id of this signal tree. The id is a randomly generated unique
     * value. The id is mainly used for identifying node ownership.
     *
     * @see SignalCommand.ScopeOwnerCommand
     * @see TreeRevision#ownerId()
     *
     * @return the tree id, not <code>null</code>
     */
    public Id id() {
        return id;
    }

    /**
     * Gets the lock that is used for protecting the integrity of this signal
     * tree. Locking is in general handled automatically by the tree but needs
     * to be handled externally when applying transactions so that all trees
     * participating in a transaction are locked before starting to evaluate the
     * transaction.
     *
     * @return the tree lock instance, not <code>null</code>
     */
    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Checks whether the tree lock is currently held.
     *
     * @return <code>true</code> if the lock is held by the current thread
     */
    protected boolean hasLock() {
        return lock.isHeldByCurrentThread();
    }

    /**
     * Runs a supplier while holding the lock and returns the provided value.
     *
     * @param <T>
     *            the supplier type
     * @param action
     *            the supplier to run, not <code>null</code>
     * @return the value returned by the supplier
     */
    protected <T> T getWithLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs an action while holding the lock.
     *
     * @param action
     *            the action to run, not <code>null</code>
     */
    protected void runWithLock(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wraps the provided action to run it while holding the lock.
     *
     * @param action
     *            the action to wrap, not <code>null</code>
     * @return a runnable that runs the provided action while holding the lock,
     *         not <code>null</code>
     */
    protected Runnable wrapWithLock(Runnable action) {
        return () -> runWithLock(action);
    }

    /**
     * Registers an observer for a node in this tree. The observer will be
     * invoked the next time the corresponding node is updated in the submitted
     * snapshot. The observer is removed when invoked and needs to be registered
     * again if it's still relevant unless it returns <code>true</code>. It is
     * safe to register the observer again from within the callback.
     *
     * @param nodeId
     *            the id of the node to observe, not <code>null</code>
     * @param observer
     *            the callback to run when the node has changed, not
     *            <code>null</code>
     * @return a callback that can be used to remove the observer before it's
     *         triggered, not <code>null</code>
     */
    public Runnable observeNextChange(Id nodeId, TransientListener observer) {
        assert nodeId != null;
        assert observer != null;

        return getWithLock(() -> {
            assert submitted().nodes().containsKey(nodeId);

            List<TransientListener> list = observers.computeIfAbsent(nodeId,
                    ignore -> new ArrayList<>());

            list.add(observer);

            return wrapWithLock(() -> list.remove(observer));
        });
    }

    /**
     * Notify all observers that are affected by changes between two snapshots.
     * All notified observers are removed. It is safe for an observer to
     * register itself again when it is invoked.
     *
     * @see #observeNextChange(Id, TransientListener)
     *
     * @param oldSnapshot
     *            the old snapshot, not <code>null</code>
     * @param newSnapshot
     *            the new snapshot, not <code>null</code>
     */
    protected void notifyObservers(Snapshot oldSnapshot, Snapshot newSnapshot) {
        if (oldSnapshot == newSnapshot) {
            return;
        }

        runWithLock(() -> {
            Map.copyOf(observers).forEach((nodeId, list) -> {
                Data oldNode = oldSnapshot.data(nodeId).orElse(Node.EMPTY);
                Data newNode = newSnapshot.data(nodeId).orElse(Node.EMPTY);

                if (oldNode != newNode) {
                    List<TransientListener> copy = List.copyOf(list);

                    /*
                     * Assuming there will immediately be a new observer for the
                     * same node so not clearing the map entry.
                     */
                    list.clear();

                    for (TransientListener observer : copy) {
                        boolean listenToNext = observer.invoke(false);
                        if (listenToNext) {
                            list.add(observer);
                        }
                    }
                }
            });
        });
    }

    /**
     * Gets the current snapshot based on all confirmed and submitted commands.
     *
     * @return the submitted snapshot, not <code>null</code>
     */
    public abstract Snapshot submitted();

    /**
     * Gets the current snapshot based on all confirmed commands. This snapshot
     * does not contain changes from commands that have been submitted but not
     * yet confirmed.
     *
     * @return the confirmed snapshot, not <code>null</code>
     */
    public abstract Snapshot confirmed();

    /**
     * Applies a single command to this tree. This is a shorthand for committing
     * only a single command.
     *
     * @param command
     *            the command to apply, not <code>null</code>
     * @param resultHandler
     *            a result handler that will be notified when the command is
     *            confirmed, not <code>null</code> to ignore the result
     */
    public void commitSingleCommand(SignalCommand command,
            Consumer<CommandResult> resultHandler) {
        assert command != null;

        CommandsAndHandlers commands = new CommandsAndHandlers(command,
                resultHandler);

        runWithLock(() -> {
            PendingCommit commit = prepareCommit(commands);
            if (commit.canCommit()) {
                commit.applyChanges();
                commit.publishChanges();
            } else {
                commit.markAsAborted();
            }
        });
    }

    /**
     * Applies a single command to this tree without listening for the result.
     *
     * @see #commitSingleCommand(SignalCommand, Consumer)
     *
     * @param command
     *            the command to apply, not <code>null</code>
     */
    public void commitSingleCommand(SignalCommand command) {
        commitSingleCommand(command, null);
    }

    /**
     * Starts the process of committing a set of changes. The returned instance
     * defines callbacks for continuing the commit procedure.
     * <p>
     * Note that this method expects that the caller has acquired the tree lock
     * prior to calling the method and that the lock will remain acquired while
     * interacting with the returned object.
     *
     * @param changes
     *            the changes to commit, not <code>null</code>
     * @return callbacks for coordinating the rest of the commit sequence, not
     *         <code>null</code>
     */
    public abstract PendingCommit prepareCommit(CommandsAndHandlers changes);

    /**
     * Gets the type of this signal tree.
     *
     * @return the signal tree type, not <code>null</code>
     */
    public Type type() {
        return type;
    }

    /**
     * Registers a callback that is executed after commands are processed
     * (regardless of acceptance or rejection). It is guaranteed that the
     * callback is invoked in the order the commands are processed. Contrary to
     * the observers that are attached to a specific node by calling
     * {@link #observeNextChange}, the <code>subscriber</code> remains active
     * indefinitely until it is removed by executing the returned callback.
     *
     * @param subscriber
     *            the callback to run when a command is confirmed, not
     *            <code>null</code>
     * @return a callback that can be used to remove the subscriber, not
     *         <code>null</code>
     */
    public Runnable subscribeToProcessed(
            BiConsumer<SignalCommand, CommandResult> subscriber) {
        assert subscriber != null;
        return getWithLock(() -> {
            subscribers.add(subscriber);
            return wrapWithLock(() -> subscribers.remove(subscriber));
        });
    }

    /**
     * Notifies all subscribers after a command is processed. This method must
     * be called from a code block that holds the tree lock.
     *
     * @param commands
     *            the list of processed commands, not <code>null</code>
     * @param results
     *            the map of results for the commands, not <code>null</code>
     */
    protected void notifyProcessedCommandSubscribers(
            List<SignalCommand> commands, Map<Id, CommandResult> results) {
        assert hasLock();
        for (var command : commands) {
            for (var subscriber : subscribers) {
                subscriber.accept(command, results.get(command.commandId()));
            }
        }
    }
}
