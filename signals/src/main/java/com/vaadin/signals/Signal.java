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
package com.vaadin.signals;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.signals.impl.ComputedSignal;
import com.vaadin.signals.impl.Effect;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.Transaction.Type;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.operations.TransactionOperation;

/**
 * A signal is a reactive value holder with automatic subscription and
 * unsubscription of listeners.
 * <p>
 * Reactivity is based on {@link Signal#effect(Runnable)} callbacks that detect
 * the signals used during invocation. The callback will be run again whenever
 * there's a change to any of the signal instances used in the previous
 * invocation. Detection is based on running {@link #value()}.
 * {@link #untracked(Supplier)} can be used to read the value within an effect
 * without registering a dependency.
 * <p>
 * This interface can be used for creating simple computed signals as a lambda
 * function that uses other signals. This kind of signal is more limited than
 * {@link #computed(Supplier)} since it doesn't cache its value.
 *
 * @param <T>
 *            the signal value type
 */
@FunctionalInterface
public interface Signal<T> {
    /**
     * Gets the current value of this signal. The value is read in a way that
     * takes the current transaction into account and in the case of clustering
     * also changes that have been submitted to the cluster but not yet
     * confirmed.
     * <p>
     * If the signal implementation supports transactions, then reading the
     * value in a regular (i.e. {@link Type#STAGED}) transaction makes the
     * transaction depend on the value so that the transaction fails in case the
     * signal value is changed concurrently.
     * <p>
     * Reading the value inside an {@link #effect(Runnable)} or
     * {@link #computed(Supplier)} callback sets up that effect or computed
     * signal to depend on the signal.
     *
     * @return the signal value
     */
    T value();

    /**
     * Reads the value without setting up any dependencies. This method returns
     * the same value as {@link #value()} but without creating a dependency when
     * used inside a transaction, effect or computed signal.
     *
     * @return the signal value
     */
    default T peek() {
        /*
         * Subclasses are encouraged to use an approach with less overhead than
         * what this very generic implementation can do.
         */
        return untracked(() -> value());
    }

    /**
     * Creates a simple computed signal based on a mapper function that is
     * passed the value of this signal. If the mapper function accesses other
     * signal values, then the computed signal will also depend on those
     * signals.
     * <p>
     * The computed signal does not perform any caching but will instead run the
     * callback every time the signal value is read. Use
     * {@link #computed(Supplier)} to create a computed signal that caches the
     * result of running the callback until the value of any dependency changes.
     *
     * @param <C>
     *            the computed signal type
     * @param mapper
     *            the mapper function to use, not <code>null</code>
     * @return the computed signal, not <code>null</code>
     */
    default <C> Signal<C> map(Function<T, C> mapper) {
        return () -> mapper.apply(value());
    }

    /*
     * These are a bunch of public access points to API that is originally
     * implemented in classes in the .impl package. These methods are unit
     * tested from the same class that tests the implementation class.
     */

    /**
     * Creates a signal effect with the given action. The action is run when the
     * effect is created and is subsequently run again whenever there's a change
     * to any signal value that was read during the last invocation.
     *
     * @param action
     *            the effect action to use, not <code>null</code>
     * @return a callback used to close the effect so that it no longer listens
     *         to signal changes, not <code>null</code>
     */
    static Runnable effect(Runnable action) {
        Effect effect = new Effect(Objects.requireNonNull(action));
        return effect::dispose;
    }

    /**
     * Creates a new computed signal with the given computation callback. A
     * computed signal behaves like a regular signal except that the value is
     * not directly set but instead computed from other signals. The computed
     * signal is automatically updated if any of the used signals are updated.
     * The computation is lazy so that it only runs when its value is accessed
     * and only if the previously computed value might have been invalidated by
     * dependent signal changes. If the computation callback throws a
     * {@link RuntimeException}, then that exception will be re-thrown when
     * accessing the signal value. An {@link Signal#effect(Runnable) effect} or
     * computed signal that uses the value from a computed signal will not be
     * invalidated if the computation is run again but produces the same value
     * as before.
     *
     * @param <T>
     *            the signal type
     * @param computation
     *            the computation callback, not <code>null</code>
     * @return the computed signal, not <code>null</code>
     */
    static <T> Signal<T> computed(Supplier<T> computation) {
        return new ComputedSignal<>(computation);
    }

    /**
     * Runs the provided supplier in a transaction. All signal operations
     * performed within the transaction will be staged and atomically committed
     * at the end of the transaction. The commit fails and doesn't apply any of
     * the commands if any of the commands fail. Reading a signal value within a
     * transaction will make the transaction depend on that value so that the
     * transaction fails if the signal value has been changed concurrently.
     * <p>
     * The value returned by the supplier will be available from the returned
     * operation instance. The result of the operation will be set based on
     * whether the transaction was successfully committed once the status is
     * confirmed.
     *
     * @see #runInTransaction(Runnable)
     *
     * @param <T>
     *            the type returned by the supplier
     * @param transactionTask
     *            the supplier to run, not <code>null</code>
     * @return a transaction operation containing the supplier return value and
     *         the eventual result
     */
    static <T> TransactionOperation<T> runInTransaction(
            Supplier<T> transactionTask) {
        return Transaction.runInTransaction(transactionTask);
    }

    /**
     * Runs the provided runnable in a transaction. All signal operations
     * performed within the transaction will be staged and atomically committed
     * at the end of the transaction. The commit fails and doesn't apply any of
     * the commands if any of the commands fail. Reading a signal value within a
     * transaction will make the transaction depend on that value so that the
     * transaction fails if the signal value has been changed concurrently.
     * <p>
     * The result of the operation will be set based on whether the transaction
     * was successfully committed once the status is confirmed.
     *
     * @see Signal#runInTransaction(Supplier)
     *
     * @param transactionTask
     *            the runnable to run, not <code>null</code>
     * @return a transaction operation containing the supplier return value and
     *         the eventual result
     */
    static TransactionOperation<Void> runInTransaction(
            Runnable transactionTask) {
        return Transaction.runInTransaction(transactionTask);
    }

    /**
     * Runs the given supplier outside any transaction and returns the supplied
     * value. The current transaction will be restored after the task has been
     * run.
     *
     * @param <T>
     *            the supplier type
     * @param task
     *            the supplier to run, not <code>null</code>
     * @return the value returned from the supplier
     */
    static <T> T runWithoutTransaction(Supplier<T> task) {
        return Transaction.runWithoutTransaction(task);
    }

    /**
     * Runs the given task outside any transaction. The current transaction will
     * be restored after the task has been run.
     *
     * @param task
     *            the task to run, not <code>null</code>
     */
    static void runWithoutTransaction(Runnable task) {
        Transaction.runWithoutTransaction(task);
    }

    /**
     * Runs the given supplier without tracking dependencies for signals that
     * are read within the supplier. This has the same effect as
     * {@link AbstractSignal#peek()} but is effective for an entire code block
     * rather than just a single invocation.
     *
     * @param <T>
     *            the supplier type
     * @param task
     *            the supplier task to run, not <code>null</code>
     * @return the value returned from the supplier
     */
    static <T> T untracked(Supplier<T> task) {
        /*
         * Note that there's no Runnable overload since the whole point of
         * untracked is to read values.
         */
        return UsageTracker.untracked(task);
    }
}
