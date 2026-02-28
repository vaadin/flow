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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableFunction;

/**
 * An operation that can be cancelled.
 *
 * @param <T>
 *            the result type
 */
public class CancelableOperation<T extends @Nullable Object>
        extends SignalOperation<T> {
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

    /**
     * Creates a new cancelable operation that transforms the result value using
     * the given mapper function. Cancelling the returned operation will also
     * cancel this operation. If this operation fails, the mapped operation will
     * also fail with the same error.
     *
     * @param <R>
     *            the mapped result type
     * @param mapper
     *            the function to transform the result value, not
     *            <code>null</code>
     * @return a new cancelable operation with the mapped result, not
     *         <code>null</code>
     */
    @Override
    public <R> CancelableOperation<R> map(SerializableFunction<T, R> mapper) {
        CancelableOperation<T> parent = this;
        CancelableOperation<R> mapped = new CancelableOperation<>() {
            @Override
            public void cancel() {
                super.cancel();
                parent.cancel();
            }

            @Override
            public boolean isCancelled() {
                return parent.isCancelled();
            }
        };
        forwardMappedResult(mapped, mapper);
        return mapped;
    }
}
