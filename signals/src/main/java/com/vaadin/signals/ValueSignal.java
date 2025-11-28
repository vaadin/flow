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

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.vaadin.signals.Node.Data;
import com.vaadin.signals.impl.SignalTree;
import com.vaadin.signals.impl.SynchronousSignalTree;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A signal containing a value. The value is updated as a single atomic change.
 * It's recommended to use immutable values and this is partially enforced by
 * the way a new instance is created from the underlying JSON data every time
 * the value is read.
 *
 * @param <T>
 *            the signal value type
 */
public class ValueSignal<T> extends AbstractSignal<T>
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
    public ValueSignal(T initialValue) {
        this(new SynchronousSignalTree(false), Id.ZERO, ANYTHING_GOES,
                (Class<T>) initialValue.getClass());
        value(initialValue);
    }

    /**
     * Creates a new value signal of the given type with no value. The signal
     * does not support clustering.
     *
     * @param valueType
     *            the value type, not <code>null</code>
     */
    public ValueSignal(Class<T> valueType) {
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
    protected ValueSignal(SignalTree tree, Id id,
            Predicate<SignalCommand> validator, Class<T> valueType) {
        super(tree, id, validator);
        this.valueType = Objects.requireNonNull(valueType);
    }

    @Override
    public SignalOperation<T> value(T value) {
        assert value == null || valueType.isInstance(value);

        return submit(
                new SignalCommand.SetCommand(Id.random(), id(), toJson(value)),
                success -> nodeValue(success.onlyUpdate().oldNode(),
                        valueType));
    }

    @Override
    protected T extractValue(Data data) {
        if (data == null) {
            return null;
        } else {
            return nodeValue(data, valueType);
        }
    }

    @Override
    protected Object usageChangeValue(Data data) {
        return data.value();
    }

    @Override
    public SignalOperation<Void> replace(T expectedValue, T newValue) {
        var condition = new SignalCommand.ValueCondition(Id.random(), id(),
                toJson(expectedValue));
        var set = new SignalCommand.SetCommand(Id.random(), id(),
                toJson(newValue));

        return submit(new SignalCommand.TransactionCommand(Id.random(),
                List.of(condition, set)));
    }

    @Override
    public CancelableOperation<T> update(UnaryOperator<T> updater) {
        CancelableOperation<T> operation = new CancelableOperation<>();

        tryUpdate(updater, operation);

        return operation;
    }

    private void tryUpdate(UnaryOperator<T> updater,
            CancelableOperation<T> operation) {
        if (operation.isCancelled()) {
            operation.result().cancel(false);
            return;
        }

        /*
         * Cannot easily optimize this to directly submit a transaction command
         * since we need the previous value from the set command result
         */
        SignalOperation<T> setOperation = Transaction.runInTransaction(() -> {
            T value = peek();
            verifyValue(value);

            T newValue = updater.apply(value);
            return value(newValue);
        }).returnValue();

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
     * {@link Signal#runInTransaction(Runnable) transaction}. The result of the
     * returned operation will be resolved as successful if the expected value
     * was present and resolved as unsuccessful if any other value was present
     * when the operation is processed.
     *
     * @param expectedValue
     *            the expected value
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyValue(T expectedValue) {
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
    public ValueSignal<T> withValidator(Predicate<SignalCommand> validator) {
        return new ValueSignal<>(tree(), id(), mergeValidators(validator),
                valueType);
    }

    @Override
    public ValueSignal<T> asReadonly() {
        /*
         * While this method could semantically be declared to return a less
         * specific type that doesn't provide mutator methods, that would also
         * remove access to e.g. the verifyValue method.
         */
        return withValidator(anything -> false);
    }

    public NodeSignal asNode() {
        // Override to make public
        return super.asNode();
    }

    @Override
    public boolean equals(Object obj) {
        // Explicitly checking getClass() to avoid accidental equality with
        // NumberSignal
        return this == obj || obj instanceof ValueSignal<?> other
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
        return "ValueSignal[" + peek() + "]";
    }
}
