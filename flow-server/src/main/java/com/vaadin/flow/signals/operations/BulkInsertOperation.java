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
package com.vaadin.flow.signals.operations;

import java.util.List;
import java.util.Objects;

import com.vaadin.flow.signals.Signal;

/**
 * An operation that inserts multiple child signals into a list as a single
 * atomic batch. Unlike using individual {@link InsertOperation} instances, this
 * operation tracks the entire batch with a single {@link #result()} future.
 *
 * @param <T>
 *            the type of the newly inserted signals
 */
public class BulkInsertOperation<T extends Signal<?>>
        extends SignalOperation<Void> {

    private final List<T> signals;

    /**
     * Creates a new bulk insert operation with the given list of inserted
     * signal instances.
     *
     * @param signals
     *            an unmodifiable list of the newly inserted signal instances,
     *            not <code>null</code>
     */
    public BulkInsertOperation(List<T> signals) {
        this.signals = Objects.requireNonNull(signals);
    }

    /**
     * Gets the list of newly inserted signal instances. The instances can be
     * used immediately even in cases where the result of the operation is not
     * immediately confirmed.
     *
     * @return an unmodifiable list of the newly inserted signal instances, not
     *         <code>null</code>
     */
    public List<T> signals() {
        return signals;
    }
}
