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
import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableIntFunction;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.function.CommandValidator;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.shared.impl.SignalTree;

/**
 * A signal containing a numeric value. The value is updated as a single atomic
 * change. In addition to the regular {@link SharedValueSignal} operation, this
 * class also supports atomically incrementing the value.
 */
public class SharedNumberSignal extends SharedValueSignal<Double> {

    /**
     * Creates a new number signal with a zero value. The signal does not
     * support clustering.
     */
    public SharedNumberSignal() {
        super(0d);
    }

    /**
     * Creates a new number signal with the given value. The signal does not
     * support clustering.
     *
     * @param initialValue
     *            the initial value
     */
    public SharedNumberSignal(double initialValue) {
        super(Double.valueOf(initialValue));
    }

    /**
     * Creates a new number signal instance with the given id and validator for
     * the given signal tree.
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
     */
    protected SharedNumberSignal(SignalTree tree, Id id,
            CommandValidator validator) {
        super(tree, id, validator, Double.class);
    }

    /**
     * Atomically increments the value of this signal by the given delta amount.
     * The value is decremented if the delta is negative. The result of the
     * returned operation will be resolved with the update value at the time
     * when this operation was confirmed.
     *
     *
     * @param delta
     *            the increment amount
     * @return an operation containing the eventual result
     */
    public SignalOperation<Double> incrementBy(double delta) {
        return submit(
                new SignalCommand.IncrementCommand(Id.random(), id(), delta),
                success -> nodeValue(
                        Objects.requireNonNull(success.onlyUpdate().newNode()),
                        Double.class));
    }

    @Override
    public Double get() {
        Double value = super.get();
        if (value == null) {
            return Double.valueOf(0);
        }
        return value;
    }

    @Override
    public Double peek() {
        Double value = super.peek();
        if (value == null) {
            return Double.valueOf(0);
        }
        return value;
    }

    @Override
    public Double peekConfirmed() {
        Double value = super.peekConfirmed();
        if (value == null) {
            return Double.valueOf(0);
        }
        return value;
    }

    /**
     * Gets the value of this signal as an integer. This method works in the
     * same way was {@link #get()} with regards to transactions and dependency
     * tracking.
     *
     * @return the signal value as an integer
     */
    public int getAsInt() {
        return get().intValue();
    }

    /**
     * Sets the value of this signal as an integer. This method works in the
     * same way was the regular value setter with regards to transactions.
     *
     * @param value
     *            the integer value to set
     * @return an operation containing the eventual result
     */
    public SignalOperation<Double> set(int value) {
        return set(Double.valueOf(value));
    }

    /**
     * Wraps this signal with a validator. The validator is used to check all
     * value changing commands issued through the new signal instance. If this
     * signal has a validator, then the new signal will use both validators.
     * Note that due to the way validators are retained by {@link #asNode()},
     * there's a possibility that the validator also receives commands that
     * cannot be directly issued for a number signal.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @param validator
     *            the validator to use, not <code>null</code>
     * @return a new number signal that uses the validator, not
     *         <code>null</code>
     */
    public SharedNumberSignal withValidator(CommandValidator validator) {
        return new SharedNumberSignal(tree(), id(), mergeValidators(validator));
    }

    /**
     * Wraps this signal to not accept changes.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @return the new readonly number signal, not <code>null</code>
     */
    public SharedNumberSignal asReadonly() {
        /*
         * While this method could semantically be declared to return a less
         * specific type that doesn't provide mutator methods, that would also
         * remove access to e.g. the getAsInt method.
         */
        return withValidator(anything -> false);
    }

    /**
     * Creates a computed signal based on an integer mapper function that is
     * passed the value of this signal. If the mapper function accesses other
     * signal values, then the computed signal will also depend on those
     * signals.
     *
     * @param <C>
     *            the computed signal type
     * @param mapper
     *            the integer mapper function to use, not <code>null</code>
     * @return the computed signal, not <code>null</code>
     */
    public <C> Signal<C> mapIntValue(SerializableIntFunction<C> mapper) {
        return map(doubleValue -> {
            if (doubleValue == null) {
                return null;
            }
            return mapper.apply(doubleValue.intValue());
        });
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SharedNumberSignal other
                && Objects.equals(tree(), other.tree())
                && Objects.equals(id(), other.id())
                && Objects.equals(validator(), other.validator());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree(), id(), validator());
    }

    @Override
    public String toString() {
        return "SharedNumberSignal[" + peek() + "]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        LoggerFactory.getLogger(SharedNumberSignal.class).warn(
                "Serializing SharedNumberSignal. Sharing signals across a cluster is not yet implemented.");
        out.defaultWriteObject();
    }
}
