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

/**
 * A signal operation representing a transaction and the return value from the
 * transaction callback. The {@link #result()} for a transaction doesn't carry
 * any value. Note that in the case of write-through transactions, the result
 * will always be successful even if operations applied within the transaction
 * were not successful.
 *
 * @param <T>
 *            the transaction return value type
 */
public class TransactionOperation<T> extends SignalOperation<Void> {
    private final T returnValue;

    /**
     * Creates a new transaction operation with the provided return value.
     *
     * @param returnValue
     *            the transaction callback return value
     */
    public TransactionOperation(T returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * Gets the return value from the transaction callback. <code>null</code> is
     * used as the return value when the the transaction callback is a
     * {@link Runnable}.
     *
     * @return the operation callback return value
     */
    public T returnValue() {
        return returnValue;
    }
}
