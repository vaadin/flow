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
package com.vaadin.signals.operations;

import java.util.Objects;

import com.vaadin.signals.Signal;

/**
 * An operation that inserts a new child signal into a list or map. In addition
 * to the regular signal operation, there's also direct access to the newly
 * inserted child signal.
 *
 * @param <T>
 *            the type of the newly inserted signal
 */
public class InsertOperation<T extends Signal<?>>
        extends SignalOperation<Void> {

    private final T newSignal;

    /**
     * Creates a new insert operation based on the new signal instance.
     *
     * @param newSignal
     *            the new signal instance, not <code>null</code>
     */
    public InsertOperation(T newSignal) {
        this.newSignal = Objects.requireNonNull(newSignal);
    }

    /**
     * Gets the newly inserted signal instance. The instance can be used
     * immediately even in cases where the result of the operation is not
     * immediately confirmed.
     *
     * @return the newly inserted signal instance, not <code>null</code>
     */
    public T signal() {
        return newSignal;
    }
}
