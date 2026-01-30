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
package com.vaadin.signals.impl;

import java.util.Objects;

import com.vaadin.signals.WritableSignal;
import com.vaadin.signals.function.SignalMapper;
import com.vaadin.signals.function.SignalSetter;
import com.vaadin.signals.function.SignalUpdater;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A writable signal that provides a two-way mapped view of another writable
 * signal. Reading the signal applies a getter function to extract a child value
 * from the parent. Writing to the signal uses a setter function to update the
 * parent signal with the new child value.
 * <p>
 * This enables patterns like mapping a single field from a record or bean to a
 * separate writable signal that can be used with two-way bindings.
 *
 * @param <P>
 *            the parent signal value type
 * @param <C>
 *            the child (this signal's) value type
 */
public class MappedWritableSignal<P, C> implements WritableSignal<C> {

    private final WritableSignal<P> parent;
    private final SignalMapper<P, C> getter;
    private final SignalSetter<P, C> setter;

    /**
     * Creates a new mapped writable signal.
     *
     * @param parent
     *            the parent signal to map, not <code>null</code>
     * @param getter
     *            the function to extract the child value from the parent, not
     *            <code>null</code>
     * @param setter
     *            the function to create a new parent value given the current
     *            parent and new child value, not <code>null</code>
     */
    public MappedWritableSignal(WritableSignal<P> parent,
            SignalMapper<P, C> getter, SignalSetter<P, C> setter) {
        this.parent = Objects.requireNonNull(parent);
        this.getter = Objects.requireNonNull(getter);
        this.setter = Objects.requireNonNull(setter);
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
        return parent
                .update(parentValue -> setter.set(parentValue, newChildValue))
                .map(oldParent -> getter.map(oldParent));
    }

    @Override
    public SignalOperation<Void> replace(C expectedValue, C newValue) {
        P originalParentValue = parent.peek();
        C oldChildValue = getter.map(originalParentValue);
        if (!Objects.equals(oldChildValue, expectedValue)) {
            return new SignalOperation<>(
                    new SignalOperation.Error<>("Unexpected child value"));
        }
        return parent.replace(originalParentValue,
                setter.set(originalParentValue, newValue));
    }

    @Override
    public CancelableOperation<C> update(SignalUpdater<C> childUpdater) {
        Objects.requireNonNull(childUpdater);
        return parent.update(parentValue -> {
            C currentChildValue = getter.map(parentValue);
            C newChildValue = childUpdater.update(currentChildValue);
            return setter.set(parentValue, newChildValue);
        }).map(oldParent -> getter.map(oldParent));
    }
}
