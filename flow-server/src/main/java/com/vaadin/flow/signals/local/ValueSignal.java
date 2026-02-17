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
import com.vaadin.flow.signals.WritableSignal;
import com.vaadin.flow.signals.function.SignalMapper;
import com.vaadin.flow.signals.function.SignalModifier;
import com.vaadin.flow.signals.function.SignalUpdater;
import com.vaadin.flow.signals.function.ValueMerger;
import com.vaadin.flow.signals.function.ValueModifier;
import com.vaadin.flow.signals.impl.MappedModifySignal;
import com.vaadin.flow.signals.impl.Transaction;
import com.vaadin.flow.signals.operations.CancelableOperation;
import com.vaadin.flow.signals.operations.SignalOperation;

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
public class ValueSignal<T> extends AbstractLocalSignal<T>
        implements WritableSignal<T> {

    private boolean modifyRunning = false;
    private transient boolean modifyUsed = false;
    private transient boolean multipleThreadsDetected = false;
    private transient @Nullable Thread firstThread = null;

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

        // Track thread access for detecting unsafe mutable sharing
        Thread current = Thread.currentThread();
        if (firstThread == null) {
            firstThread = current;
        } else if (firstThread != current) {
            multipleThreadsDetected = true;
        }

        if (modifyUsed && multipleThreadsDetected) {
            throw new IllegalStateException(
                    "This ValueSignal instance has been used with modify() "
                            + "and accessed from multiple threads. This is not "
                            + "thread-safe because modify() works with mutable "
                            + "values without holding a lock while the modifier "
                            + "callback runs. Use immutable values with set(), "
                            + "replace(), or update() instead of modify() when "
                            + "the signal is shared between threads.");
        }
    }

    @Override
    public SignalOperation<T> set(@Nullable T value) {
        lock();
        try {
            checkPreconditions();

            @Nullable
            T oldValue = getSignalValue();

            setSignalValue(value);

            return new SignalOperation<>(
                    new SignalOperation.Result<>(oldValue));
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Comparison between the expected value and the new value is performed
     * using {@link #equals(Object)}.
     */
    @Override
    public SignalOperation<Void> replace(@Nullable T expectedValue,
            @Nullable T newValue) {
        lock();
        try {
            checkPreconditions();

            if (Objects.equals(expectedValue, getSignalValue())) {
                setSignalValue(newValue);
                return new SignalOperation<>(
                        new SignalOperation.Result<>(null));
            } else {
                return new SignalOperation<>(
                        new SignalOperation.Error<>("Unexpected value"));
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
     * The result of the returned operation is resolved with the same value that
     * was passed to the updater callback.
     *
     * @param updater
     *            the value update callback, not <code>null</code>
     * @return an operation containing the result
     */
    @Override
    public synchronized CancelableOperation<T> update(
            SignalUpdater<T> updater) {
        Objects.requireNonNull(updater);
        lock();
        try {
            checkPreconditions();

            @Nullable
            T oldValue = getSignalValue();
            @Nullable
            T newValue = updater.update(oldValue);
            if (newValue != oldValue) {
                setSignalValue(newValue);
            }

            CancelableOperation<T> operation = new CancelableOperation<>();
            operation.result().complete(new SignalOperation.Result<>(oldValue));
            return operation;
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
            modifyUsed = true;
            checkPreconditions();

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
     * Creates a two-way mapped signal that uses in-place modification for
     * writing. Reading the mapped signal applies the getter function to extract
     * a child value. Writing to the mapped signal uses the modifier function to
     * update this signal's value in place.
     * <p>
     * This method is named differently from
     * {@link WritableSignal#map(SignalMapper, SignalSetter)} to avoid ambiguity
     * in method overload resolution when using method references or lambdas.
     * <p>
     * This is useful for mutable bean patterns where the parent object's
     * properties are modified directly using setters. For example:
     *
     * <pre>
     * class Todo {
     *     private String text;
     *     private boolean done;
     *
     *     public boolean isDone() {
     *         return done;
     *     }
     *
     *     public void setDone(boolean done) {
     *         this.done = done;
     *     }
     * }
     *
     * ValueSignal&lt;Todo&gt; todoSignal = new ValueSignal&lt;&gt;(new Todo());
     * WritableSignal&lt;Boolean&gt; doneSignal = todoSignal.mapMutable(Todo::isDone,
     *         Todo::setDone);
     *
     * checkbox.bindValue(doneSignal, doneSignal::set); // Two-way binding
     * </pre>
     *
     * @param <C>
     *            the child (mapped) signal type
     * @param getter
     *            the function to extract the child value from this signal's
     *            value, not <code>null</code>
     * @param modifier
     *            the function to modify this signal's value in place with the
     *            new child value, not <code>null</code>
     * @return a two-way mapped signal using in-place modification, not
     *         <code>null</code>
     */
    public <C> WritableSignal<C> mapMutable(SignalMapper<T, C> getter,
            SignalModifier<T, C> modifier) {
        return new MappedModifySignal<>(this, getter, modifier);
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
