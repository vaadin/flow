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

import java.util.concurrent.CompletableFuture;

/**
 * An operation triggered on a signal instance. The result will be populated
 * when the operation has been confirmed. The type of the result depends on the
 * operation type.
 *
 * @param <T>
 *            the result type
 */
public class SignalOperation<T> {
    /**
     * The result of a signal operation.
     *
     * @param <T>
     *            the result type in case the operation was successful
     */
    public sealed interface ResultOrError<T> {
        /**
         * Checks whether this instance represents successfully applied signal
         * operation.
         *
         * @return <code>true</code> if the operation was successful,
         *         <code>false</code> if there was an error
         */
        boolean successful();
    }

    /**
     * A successful signal operation result.
     *
     * @param <T>
     *            the result type
     * @param value
     *            the result value
     */
    public static record Result<T>(T value) implements ResultOrError<T> {
        @Override
        public boolean successful() {
            return true;
        }
    }

    /**
     * A failed signal operation result.
     *
     * @param <T>
     *            the inherited result type which is not used for errors
     * @param reason
     *            the error reason message
     */
    public static record Error<T>(String reason) implements ResultOrError<T> {
        @Override
        public boolean successful() {
            return false;
        }
    }

    private final CompletableFuture<ResultOrError<T>> result = new CompletableFuture<>();

    /**
     * Creates a new signal operation.
     */
    public SignalOperation() {
    }

    /**
     * Gets the eventual operation result.
     *
     * @return a completable future with the operation result, not
     *         <code>null</code>
     */
    public CompletableFuture<ResultOrError<T>> result() {
        return result;
    }
}
