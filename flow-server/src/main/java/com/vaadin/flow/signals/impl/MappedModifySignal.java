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
package com.vaadin.flow.signals.impl;

import java.util.Objects;

import com.vaadin.flow.signals.WritableSignal;
import com.vaadin.flow.signals.function.SignalMapper;
import com.vaadin.flow.signals.function.SignalModifier;
import com.vaadin.flow.signals.function.SignalUpdater;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.operations.CancelableOperation;
import com.vaadin.flow.signals.operations.SignalOperation;

/**
 * A writable signal that provides a two-way mapped view of a
 * {@link ValueSignal} using in-place modification. Reading the signal applies a
 * getter function to extract a child value from the parent. Writing to the
 * signal uses a modifier function to update the parent value in place.
 * <p>
 * This is useful for mutable bean patterns where the parent object's properties
 * are modified directly using setters.
 *
 * @param <P>
 *            the parent signal value type
 * @param <C>
 *            the child (this signal's) value type
 */
public class MappedModifySignal<P, C> implements WritableSignal<C> {

    private final ValueSignal<P> parent;
    private final SignalMapper<P, C> getter;
    private final SignalModifier<P, C> modifier;

    /**
     * Creates a new mapped modify signal.
     *
     * @param parent
     *            the parent value signal to map, not <code>null</code>
     * @param getter
     *            the function to extract the child value from the parent, not
     *            <code>null</code>
     * @param modifier
     *            the function to modify the parent value in place with the new
     *            child value, not <code>null</code>
     */
    public MappedModifySignal(ValueSignal<P> parent, SignalMapper<P, C> getter,
            SignalModifier<P, C> modifier) {
        this.parent = Objects.requireNonNull(parent);
        this.getter = Objects.requireNonNull(getter);
        this.modifier = Objects.requireNonNull(modifier);
    }

    @Override
    public C value() {
        return getter.map(parent.value());
    }

    @Override
    public C peek() {
        return getter.map(parent.peek());
    }

    @Override
    public SignalOperation<C> value(C newChildValue) {
        C oldChildValue = getter.map(parent.peek());
        parent.modify(
                parentValue -> modifier.modify(parentValue, newChildValue));
        return new SignalOperation<>(
                new SignalOperation.Result<>(oldChildValue));
    }

    @Override
    public SignalOperation<Void> replace(C expectedValue, C newValue) {
        C currentChildValue = getter.map(parent.peek());
        if (Objects.equals(expectedValue, currentChildValue)) {
            parent.modify(
                    parentValue -> modifier.modify(parentValue, newValue));
            return new SignalOperation<>(new SignalOperation.Result<>(null));
        } else {
            return new SignalOperation<>(
                    new SignalOperation.Error<>("Unexpected child value"));
        }
    }

    @Override
    public CancelableOperation<C> update(SignalUpdater<C> childUpdater) {
        Objects.requireNonNull(childUpdater);
        C currentChildValue = getter.map(parent.peek());
        C newChildValue = childUpdater.update(currentChildValue);
        parent.modify(
                parentValue -> modifier.modify(parentValue, newChildValue));

        CancelableOperation<C> operation = new CancelableOperation<>();
        operation.result()
                .complete(new SignalOperation.Result<>(currentChildValue));
        return operation;
    }
}
