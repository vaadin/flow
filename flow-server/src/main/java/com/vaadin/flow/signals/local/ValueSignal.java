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
package com.vaadin.flow.signals.local;

import java.util.ConcurrentModificationException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.function.SignalModifier;
import com.vaadin.flow.signals.function.SignalUpdater;
import com.vaadin.flow.signals.function.ValueMerger;
import com.vaadin.flow.signals.function.ValueModifier;
import com.vaadin.flow.signals.impl.Transaction;

/**
 * A local writable signal that holds a reference to an object.
 * <p>
 * Local signals are non-serializable and intended for UI-local state only. They
 * do not participate in clustering and are simpler than shared signals.
 * <p>
 * Changing the signal to reference another immutable value is an atomic
 * operation. It is safe to concurrently read and write the signal value from
 * multiple threads.
 * <p>
 * The signal can also be used with mutable values in which case no thread
 * safety is provided. Mutations must be done through
 * {@link #modify(ValueModifier)} to ensure dependents are informed after the
 * modification is applied.
 * <p>
 * Local value signals can't be used inside signal transactions.
 * <p>
 * All operation objects returned from methods on this class are resolved
 * immediately.
 *
 * @param <T>
 *            the signal value type
 */
public class ValueSignal<T> extends AbstractLocalSignal<T> {

    private boolean modifyRunning = false;
    private transient boolean modifyUsed = false;
    private transient boolean usedWithoutSessionLock = false;

    /**
     * Creates a new value signal with the given initial value.
     *
     * @param initialValue
     *            the initial value, may be <code>null</code>
     */
    public ValueSignal(@Nullable T initialValue) {
        super(initialValue);
    }

    /**
     * Creates a new value signal with an initial value of <code>null</code>.
     */
    public ValueSignal() {
        this(null);
    }

    @Override
    protected void checkPreconditions() {
        assertLockHeld();

        if (Transaction.inTransaction()) {
            throw new IllegalStateException(
                    "ValueSignal cannot be used inside signal transactions.");
        }

        if (modifyRunning) {
            throw new ConcurrentModificationException();
        }

        // Track if the signal has ever been accessed without a locked session
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || !session.hasLock()) {
            usedWithoutSessionLock = true;
        }

        if (modifyUsed && usedWithoutSessionLock) {
            throw new IllegalStateException(
                    "This ValueSignal instance has been used with modify() "
                            + "and accessed without holding the session lock. "
                            + "This is not thread-safe because modify() works "
                            + "with mutable values without holding a lock while "
                            + "the modifier callback runs. Use immutable values "
                            + "with set(), replace(), or update() instead of "
                            + "modify() when the signal is shared between "
                            + "threads.");
        }
    }

    /**
     * Sets the value of this signal.
     * <p>
     * Setting a new value will trigger effect functions that have reads from
     * this signal.
     *
     * @param value
     *            the value to set
     */
    public void set(@Nullable T value) {
        lock();
        try {
            checkPreconditions();

            setSignalValue(value);
        } finally {
            unlock();
        }
    }

    /**
     * Sets the value of this signal if and only if the signal has the expected
     * value at the time when the operation is confirmed. This is the signal
     * counterpart to
     * {@link java.util.concurrent.atomic.AtomicReference#compareAndSet(Object, Object)}.
     * <p>
     * Comparison between the expected value and the new value is performed
     * using {@link #equals(Object)}.
     *
     * @param expectedValue
     *            the expected value
     * @param newValue
     *            the new value
     * @return <code>true</code> if the expected value was present,
     *         <code>false</code> if there was a different value
     */
    public boolean replace(@Nullable T expectedValue, @Nullable T newValue) {
        lock();
        try {
            checkPreconditions();

            if (Objects.equals(expectedValue, getSignalValue())) {
                setSignalValue(newValue);
                return true;
            } else {
                return false;
            }
        } finally {
            unlock();
        }
    }

    /**
     * Updates the signal value based on the given callback. The callback
     * receives the current signal value and returns the new value to use. This
     * implementation acquires a lock while running the updater which means that
     * it's never necessary to run the callback again. This also means that
     * canceling the returned operation will never have any effect.
     * <p>
     * Update operations cannot participate in transactions since any retry
     * would occur after the original transaction has already been committed.
     * For this reason, the whole operation completely bypasses all transaction
     * handling.
     *
     * @param updater
     *            the value update callback, not <code>null</code>
     * @return the previous value
     */
    public synchronized @Nullable T update(SignalUpdater<T> updater) {
        Objects.requireNonNull(updater);
        lock();
        try {
            checkPreconditions();

            T oldValue = getSignalValue();
            T newValue = updater.update(oldValue);
            if (newValue != oldValue) {
                setSignalValue(newValue);
            }

            return oldValue;
        } finally {
            unlock();
        }
    }

    /**
     * Runs the given callback to apply changes to a mutable referenced value
     * and then notifies dependents.
     * <p>
     * This method is only intended for cases where concurrency is limited
     * through other means, such as Vaadin's session lock. Using this method
     * concurrently with any other methods on the same instance may, but is not
     * guaranteed to, cause an {@link ConcurrentModificationException}. The
     * exception can be thrown either from this method or from the other invoked
     * method. This can happen even if the other method is safe for concurrent
     * use.
     *
     * @param modifier
     *            a callback that receives the current value to modify, not
     *            <code>null</code>
     */
    public void modify(ValueModifier<T> modifier) {
        Objects.requireNonNull(modifier);

        if (!tryLock()) {
            throw new ConcurrentModificationException();
        }
        try {
            checkPreconditions();

            modifyUsed = true;
            modifyRunning = true;
        } finally {
            unlock();
        }

        boolean completed = false;
        try {
            // Access without lock since modifyRunning prevents concurrent
            // access
            modifier.modify(getSignalValueUnsafe());

            completed = true;
        } finally {
            lock();
            try {
                modifyRunning = false;

                if (completed) {
                    setSignalValue(getSignalValue());
                }

            } finally {
                unlock();
            }
        }
    }

    /**
     * Wraps this signal to not accept changes.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @return the new readonly signal, not <code>null</code>
     */
    public Signal<T> asReadonly() {
        return () -> get();
    }

    /**
     * Creates a callback that updates this signal value using the provided
     * merger function. This is useful for creating write callbacks for
     * {@code bindValue} when working with immutable value patterns.
     * <p>
     * The merger function receives the current signal value and a new child
     * value, and should return a new signal value. This is typically a method
     * reference to a "with" style method on an immutable record or class.
     * <p>
     * Example usage with an immutable record:
     *
     * <pre>
     * record Person(String name, int age) {
     *     Person withName(String name) {
     *         return new Person(name, this.age);
     *     }
     * }
     *
     * ValueSignal&lt;Person&gt; personSignal = new ValueSignal&lt;&gt;(
     *         new Person("Alice", 30));
     * textField.bindValue(personSignal.map(Person::name),
     *         personSignal.updater(Person::withName));
     * </pre>
     *
     * @param <C>
     *            the child value type that will be provided to the callback
     * @param merger
     *            the function to create a new signal value from the old value
     *            and a new child value, not <code>null</code>
     * @return a callback that updates this signal using the merger function,
     *         not <code>null</code>
     */
    public <C> SerializableConsumer<C> updater(ValueMerger<T, C> merger) {
        Objects.requireNonNull(merger);
        return newChildValue -> update(
                currentValue -> merger.merge(currentValue, newChildValue));
    }

    /**
     * Creates a callback that modifies this signal's mutable value in place
     * using the provided modifier function. This is useful for creating write
     * callbacks for {@code bindValue} when working with mutable value patterns.
     * <p>
     * The modifier function receives the current signal value and a new child
     * value, and should modify the signal value in place. This is typically a
     * method reference to a setter method on a mutable bean.
     * <p>
     * Example usage with a mutable bean:
     *
     * <pre>
     * class Person {
     *     private String name;
     *     private int age;
     *
     *     public String getName() {
     *         return name;
     *     }
     *
     *     public void setName(String name) {
     *         this.name = name;
     *     }
     * }
     *
     * ValueSignal&lt;Person&gt; personSignal = new ValueSignal&lt;&gt;(new Person());
     * textField.bindValue(personSignal.map(Person::getName),
     *         personSignal.modifier(Person::setName));
     * </pre>
     *
     * @param <C>
     *            the child value type that will be provided to the callback
     * @param modifier
     *            the function to modify the signal value in place with a new
     *            child value, not <code>null</code>
     * @return a callback that modifies this signal using the modifier function,
     *         not <code>null</code>
     */
    public <C> SerializableConsumer<C> modifier(SignalModifier<T, C> modifier) {
        Objects.requireNonNull(modifier);
        return newValue -> modify(
                parentValue -> modifier.modify(parentValue, newValue));
    }
}
