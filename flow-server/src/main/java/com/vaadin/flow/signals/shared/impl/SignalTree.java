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
package com.vaadin.flow.signals.shared.impl;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Node;
import com.vaadin.flow.signals.Node.Data;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.function.ValueSupplier;
import com.vaadin.flow.signals.impl.LeafLock;
import com.vaadin.flow.signals.impl.TransientListener;
import com.vaadin.flow.signals.shared.impl.CommandsAndHandlers.CommandResultHandler;

/**
 * Provides thread-safe access to a tree of signal nodes and a way of listening
 * for changes to those nodes. There are two primary types of signal trees:
 * synchronous trees have their changes applied immediately whereas asynchronous
 * trees make a differences between submitted changes and changes that have been
 * asynchronously confirmed.
 * 
 * @since 25.1
 */
public abstract class SignalTree implements Serializable {
    /**
     * Receives notifications about processed signal commands and their results.
     */
    @FunctionalInterface
    public interface CommandSubscriber extends Serializable {
        /**
         * Called when a command has been processed.
         *
         * @param command
         *            the processed command, not <code>null</code>
         * @param result
         *            the command result, not <code>null</code>
         */
        void onCommandProcessed(SignalCommand command, CommandResult result);
    }

    /**
     * Collection of callbacks representing the possible stages when committing
     * a transaction. The commit is split up into stages to enable a coordinated
     * transaction that includes multiple signal trees.
     *
     * @see SignalTree#prepareCommit(CommandsAndHandlers)
     */
    public interface PendingCommit extends Serializable {
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

    /**
     * A registered node observer. Observers are notified only once the
     * notifying thread no longer holds any tree lock, so that an observer that
     * reads or writes another tree cannot form a lock-order inversion with a
     * thread doing the same in the opposite direction (see #26130).
     * <p>
     * Since notifications are delivered outside the lock, the same observer
     * could otherwise be invoked concurrently from two committing threads. The
     * pending counter serializes delivery: only the thread that bumps it from
     * zero delivers, and it keeps invoking the observer until no further
     * notification has arrived in the meantime. Observers do not receive any
     * event payload but re-read the current state when invoked, so coalescing
     * several notifications into one invocation does not lose any change.
     */
    private final class Observer implements Serializable {
        private final List<Observer> list;
        private final TransientListener listener;
        private final AtomicInteger pendingNotifications = new AtomicInteger();
        private volatile boolean removed;

        private Observer(List<Observer> list, TransientListener listener) {
            this.list = list;
            this.listener = listener;
        }

        private void scheduleNotification(boolean immediate) {
            assert hasLock();
            if (pendingNotifications.getAndIncrement() == 0) {
                deferredNotifications.get().add(() -> deliver(immediate));
            }
        }

        private void deliver(boolean immediate) {
            boolean invokeImmediate = immediate;
            int handled;
            do {
                handled = pendingNotifications.get();
                if (removed) {
                    return;
                }

                boolean listenToNext;
                try {
                    listenToNext = listener.invoke(invokeImmediate);
                } catch (RuntimeException | Error e) {
                    remove();
                    throw e;
                }
                if (!listenToNext) {
                    remove();
                    return;
                }
                invokeImmediate = false;
            } while (!pendingNotifications.compareAndSet(handled, 0));
        }

        private void remove() {
            removed = true;
            runWithLock(() -> list.remove(this));
        }
    }

    /*
     * The number of tree locks, across all trees, that the current thread holds
     * through lock().
     */
    private static final ThreadLocal<int[]> heldTreeLocks = ThreadLocal
            .withInitial(() -> new int[1]);

    /*
     * Observer notifications scheduled while the current thread holds a tree
     * lock, delivered once it has released the last one.
     */
    private static final ThreadLocal<ArrayDeque<Runnable>> deferredNotifications = ThreadLocal
            .withInitial(ArrayDeque::new);

    private final Map<Id, List<Observer>> observers = new HashMap<>();

    private final Id id = Id.random();

    private final ReentrantLock lock = new ReentrantLock();

    private final Type type;

    private final List<CommandSubscriber> subscribers = new ArrayList<>();

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
     * Acquires the tree lock. Unlike locking {@link #getLock()} directly, this
     * keeps track of the lock so that observer notifications scheduled while
     * the lock is held are delivered only when the current thread has released
     * all tree locks through {@link #unlock()}.
     */
    public void lock() {
        assertNoLeafLockHeld();
        getLock().lock();
        heldTreeLocks.get()[0]++;
    }

    /**
     * Releases the tree lock acquired through {@link #lock()}. If this was the
     * last tree lock held by the current thread, then any observer
     * notifications scheduled while holding the lock are delivered before this
     * method returns.
     */
    public void unlock() {
        getLock().unlock();
        if (--heldTreeLocks.get()[0] == 0) {
            deliverDeferredNotifications();
        }
    }

    private static void deliverDeferredNotifications() {
        ArrayDeque<Runnable> queue = deferredNotifications.get();
        RuntimeException failure = null;
        Runnable notification;
        while ((notification = queue.poll()) != null) {
            try {
                notification.run();
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
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
     * Asserts that the current thread is not holding any component
     * {@link LeafLock} while it is about to acquire this tree lock. Acquiring a
     * tree lock while holding a leaf lock is the ordering that causes ABBA
     * deadlocks (see #25166); the reverse (tree lock held while a listener
     * takes a leaf lock) is the reference direction and stays allowed.
     * Reentrant re-locks of this same tree are permitted.
     * <p>
     * Only active under {@code -ea}. This is the enforcement side of the
     * {@link LeafLock} invariant.
     */
    private void assertNoLeafLockHeld() {
        assert lock.isHeldByCurrentThread()
                || !LeafLock.isAnyHeldByCurrentThread()
                : "Acquiring a SignalTree lock while holding a component leaf lock ("
                        + LeafLock.describeHeld()
                        + "). This leaf-lock -> tree-lock ordering causes ABBA "
                        + "deadlocks; do the tree call outside the leaf lock. See #25166.";
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
    protected <T> @Nullable T getWithLock(ValueSupplier<T> action) {
        lock();
        try {
            return action.supply();
        } finally {
            unlock();
        }
    }

    /**
     * Runs an action while holding the lock.
     *
     * @param action
     *            the action to run, not <code>null</code>
     */
    protected void runWithLock(SerializableRunnable action) {
        lock();
        try {
            action.run();
        } finally {
            unlock();
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
    protected SerializableRunnable wrapWithLock(SerializableRunnable action) {
        return () -> runWithLock(action);
    }

    /**
     * Registers an observer for a node in this tree. The observer will be
     * invoked the next time the corresponding node is updated in the submitted
     * snapshot. The observer is removed when invoked and needs to be registered
     * again if it's still relevant unless it returns <code>true</code>. It is
     * safe to register the observer again from within the callback.
     * <p>
     * The observer is invoked only after the thread that applied the change has
     * released all tree locks.
     *
     * @param nodeId
     *            the id of the node to observe, not <code>null</code>
     * @param observer
     *            the callback to run when the node has changed, not
     *            <code>null</code>
     * @return a {@link Registration} that can be used to remove the observer
     *         before it's triggered, not <code>null</code>
     */
    public Registration observeNextChange(Id nodeId,
            TransientListener observer) {
        return observeNextChange(nodeId, observer, false);
    }

    /**
     * Registers an observer for a node in this tree and optionally also invokes
     * it right away. Works like
     * {@link #observeNextChange(Id, TransientListener)}, but when
     * <code>notifyImmediately</code> is <code>true</code>, the observer is also
     * invoked with <code>immediate</code> set to <code>true</code> as soon as
     * the current thread has released all tree locks. Registering and
     * scheduling the immediate invocation happen atomically so that no change
     * can be missed in between.
     *
     * @param nodeId
     *            the id of the node to observe, not <code>null</code>
     * @param observer
     *            the callback to run when the node has changed, not
     *            <code>null</code>
     * @param notifyImmediately
     *            <code>true</code> to also invoke the observer right away,
     *            <code>false</code> to only invoke it on the next change
     * @return a {@link Registration} that can be used to remove the observer
     *         before it's triggered, not <code>null</code>
     */
    public Registration observeNextChange(Id nodeId, TransientListener observer,
            boolean notifyImmediately) {
        assert nodeId != null;
        assert observer != null;

        return Objects.requireNonNull(getWithLock(() -> {
            assert submitted().nodes().containsKey(nodeId);

            List<Observer> list = observers.computeIfAbsent(nodeId,
                    ignore -> new ArrayList<>());

            Observer entry = new Observer(list, observer);
            list.add(entry);
            if (notifyImmediately) {
                entry.scheduleNotification(true);
            }

            return entry::remove;
        }));
    }

    /**
     * Notify all observers that are affected by changes between two snapshots.
     * The observers are invoked only after the current thread has released all
     * tree locks, so that an observer can't cause a lock-order inversion by
     * acquiring the lock of another tree. An observer that returns
     * <code>false</code> is removed. It is safe for an observer to register
     * itself again when it is invoked.
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

        runWithLock(() -> observers.forEach((nodeId, list) -> {
            Data oldNode = oldSnapshot.data(nodeId).orElse(Node.EMPTY);
            Data newNode = newSnapshot.data(nodeId).orElse(Node.EMPTY);

            if (oldNode != newNode) {
                list.forEach(observer -> observer.scheduleNotification(false));
            }
        }));
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
     *            confirmed, or <code>null</code> to ignore the result
     */
    public void commitSingleCommand(SignalCommand command,
            @Nullable CommandResultHandler resultHandler) {
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
     * @return a {@link Registration} that can be used to remove the subscriber,
     *         not <code>null</code>
     */
    public Registration subscribeToProcessed(CommandSubscriber subscriber) {
        assert subscriber != null;
        return Objects.requireNonNull(getWithLock(() -> {
            subscribers.add(subscriber);
            return wrapWithLock(() -> subscribers.remove(subscriber))::run;
        }));
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
            CommandResult result = results.get(command.commandId());
            if (result == null) {
                throw new IllegalStateException(
                        "Missing result for command " + command.commandId());
            }
            for (var subscriber : subscribers) {
                subscriber.onCommandProcessed(command, result);
            }
        }
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        if (this instanceof AsynchronousSignalTree) {
            // Throwing here instead of throwing from AsynchronousSignalTree to
            // avoid possible ConcurrentModificationException due to fields in
            // SignalTree may be accessed before
            // AsynchronousSignalTree.writeObject is reached while serializing.
            throw new NotSerializableException(
                    "Shared Signal is a shared object that cannot be serialized: "
                            + "it is tied to a specific runtime environment and would "
                            + "leak other sessions if included in session serialization.");
        } else {
            out.defaultWriteObject();
        }
    }
}
