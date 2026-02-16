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
package com.vaadin.flow.signals.shared;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Node.Data;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.WritableSignal;
import com.vaadin.flow.signals.function.CommandValidator;
import com.vaadin.flow.signals.function.SignalUpdater;
import com.vaadin.flow.signals.function.TransactionTask;
import com.vaadin.flow.signals.function.ValueMerger;
import com.vaadin.flow.signals.impl.Transaction;
import com.vaadin.flow.signals.operations.CancelableOperation;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.shared.impl.SignalTree;
import com.vaadin.flow.signals.shared.impl.SynchronousSignalTree;

/**
 * A signal containing a value. The value is updated as a single atomic change.
 * It's recommended to use immutable values and this is partially enforced by
 * the way a new instance is created from the underlying JSON data every time
 * the value is read.
 *
 * @param <T>
 *            the signal value type
 */
public class SharedValueSignal<T> extends AbstractSignal<T>
        implements WritableSignal<T> {
    private final Class<T> valueType;

    /**
     * Creates a new value signal with the given initial value. The type of the
     * signal will be based on the type ({@link #getClass()}) of the initial
     * value instance. The signal does not support clustering.
     *
     * @param initialValue
     *            the initial value to use, not <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public SharedValueSignal(T initialValue) {
        this(new SynchronousSignalTree(false), Id.ZERO, ANYTHING_GOES,
                (Class<T>) initialValue.getClass());
        set(initialValue);
    }

    /**
     * Creates a new value signal of the given type with no value. The signal
     * does not support clustering.
     *
     * @param valueType
     *            the value type, not <code>null</code>
     */
    public SharedValueSignal(Class<T> valueType) {
        this(new SynchronousSignalTree(false), Id.ZERO, ANYTHING_GOES,
                Objects.requireNonNull(valueType));
    }

    /**
     * Creates a new value signal instance with the given id and validator for
     * the given signal tree with the given value type.
     *
     * @param tree
     *            the signal tree that contains the value for this signal, not
     *            <code>null</code>
     * @param id
     *            the id of the signal node within the signal tree, not
     *            <code>null</code>
     * @param validator
     *            the validator to check operations submitted to this singal,
     *            not <code>null</code>
     * @param valueType
     *            the value type, not <code>null</code>
     */
    protected SharedValueSignal(SignalTree tree, Id id,
            CommandValidator validator, Class<T> valueType) {
        super(tree, id, validator);
        this.valueType = Objects.requireNonNull(valueType);
    }

    @Override
    public SignalOperation<T> set(@Nullable T value) {
        assert value == null || valueType.isInstance(value);

        return submit(
                new SignalCommand.SetCommand(Id.random(), id(), toJson(value)),
                success -> nodeValue(
                        Objects.requireNonNull(success.onlyUpdate().oldNode()),
                        valueType));
    }

    @Override
    protected @Nullable T extractValue(@Nullable Data data) {
        if (data == null) {
            return null;
        } else {
            return nodeValue(data, valueType);
        }
    }

    @Override
    protected @Nullable Object usageChangeValue(Data data) {
        return data.value();
    }

    @Override
    public SignalOperation<Void> replace(@Nullable T expectedValue,
            @Nullable T newValue) {
        var condition = new SignalCommand.ValueCondition(Id.random(), id(),
                toJson(expectedValue));
        var set = new SignalCommand.SetCommand(Id.random(), id(),
                toJson(newValue));

        return submit(new SignalCommand.TransactionCommand(Id.random(),
                List.of(condition, set)));
    }

    @Override
    public CancelableOperation<T> update(SignalUpdater<T> updater) {
        CancelableOperation<T> operation = new CancelableOperation<>();

        tryUpdate(updater, operation);

        return operation;
    }

    private void tryUpdate(SignalUpdater<T> updater,
            CancelableOperation<T> operation) {
        if (operation.isCancelled()) {
            operation.result().cancel(false);
            return;
        }

        /*
         * Cannot easily optimize this to directly submit a transaction command
         * since we need the previous value from the set command result
         */
        SignalOperation<T> setOperation = Objects
                .requireNonNull(Transaction.runInTransaction(() -> {
                    T value = peek();
                    verifyValue(value);

                    T newValue = updater.update(value);
                    return set(newValue);
                }).returnValue());

        setOperation.result().whenComplete((result, error) -> {
            if (error != null) {
                operation.result().completeExceptionally(error);
            } else if (result.successful()) {
                operation.result().complete(result);
            } else {
                tryUpdate(updater, operation);
            }
        });
    }

    /**
     * Checks that this signal has the expected value. This operation is only
     * meaningful to use as a condition in a
     * {@link com.vaadin.flow.signals.Signal#runInTransaction(TransactionTask)
     * transaction}. The result of the returned operation will be resolved as
     * successful if the expected value was present and resolved as unsuccessful
     * if any other value was present when the operation is processed.
     *
     * @param expectedValue
     *            the expected value
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyValue(@Nullable T expectedValue) {
        return submit(new SignalCommand.ValueCondition(Id.random(), id(),
                toJson(expectedValue)));
    }

    /**
     * Wraps this signal with a validator. The validator is used to check all
     * value changing commands issued through the new signal instance. If this
     * signal has a validator, then the new signal will use both validators.
     * Note that due to the way validators are retained by {@link #asNode()},
     * there's a possibility that the validator also receives commands that
     * cannot be directly issued for a value signal.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @param validator
     *            the validator to use, not <code>null</code>
     * @return a new value signal that uses the validator, not <code>null</code>
     */
    public SharedValueSignal<T> withValidator(CommandValidator validator) {
        return new SharedValueSignal<>(tree(), id(), mergeValidators(validator),
                valueType);
    }

    @Override
    public SharedValueSignal<T> asReadonly() {
        /*
         * While this method could semantically be declared to return a less
         * specific type that doesn't provide mutator methods, that would also
         * remove access to e.g. the verifyValue method.
         */
        return withValidator(CommandValidator.REJECT_ALL);
    }

    public SharedNodeSignal asNode() {
        // Override to make public
        return super.asNode();
    }

    @Override
    public boolean equals(Object obj) {
        // Explicitly checking getClass() to avoid accidental equality with
        // SharedNumberSignal
        return this == obj || obj instanceof SharedValueSignal<?> other
                && Objects.equals(tree(), other.tree())
                && Objects.equals(id(), other.id())
                && Objects.equals(validator(), other.validator())
                && Objects.equals(valueType, other.valueType)
                && Objects.equals(getClass(), other.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree(), id(), validator(), valueType);
    }

    @Override
    public String toString() {
        return "SharedValueSignal[" + peek() + "]";
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
     * SharedValueSignal&lt;Person&gt; personSignal = new SharedValueSignal&lt;&gt;(
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        LoggerFactory.getLogger(SharedValueSignal.class).warn(
                "Serializing SharedValueSignal. Sharing signals across a cluster is not yet implemented.");
        out.defaultWriteObject();
    }
}
