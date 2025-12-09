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
 * An operation that can be cancelled.
 *
 * @param <T>
 *            the result type
 */
public class CancelableOperation<T> extends SignalOperation<T> {
    private volatile boolean cancelled = false;

    /**
     * Creates a new cancelable operation.
     */
    public CancelableOperation() {
    }

    /**
     * Cancels this operation. Note that there's still a possibility that the
     * operation will be successfully resolved rather than canceled if
     * resolution and cancel happen concurrently.
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * Checks whether this operation has been canceled.
     *
     * @return <code>true</code> if the operation is canceled
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
