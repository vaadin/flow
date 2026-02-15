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

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableFunction;

/**
 * An operation triggered on a signal instance. The result will be populated
 * when the operation has been confirmed. The type of the result depends on the
 * operation type.
 *
 * @param <T>
 *            the result type
 */
public class SignalOperation<T> implements Serializable {
    /**
     * The result of a signal operation.
     *
     * @param <T>
     *            the result type in case the operation was successful
     */
    public sealed interface ResultOrError<T> extends Serializable {
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
    public record Result<T>(@Nullable T value) implements ResultOrError<T> {
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
    public record Error<T>(String reason) implements ResultOrError<T> {
        @Override
        public boolean successful() {
            return false;
        }
    }

    private final CompletableFuture<ResultOrError<T>> result;

    /**
     * Creates a new signal operation that will get a result later.
     */
    public SignalOperation() {
        result = new CompletableFuture<>();
    }

    /**
     * Creates a new signal operation that already has a result.
     * 
     * @param resultOrError
     *            the result of the operation, not <code>null</code>
     */
    public SignalOperation(ResultOrError<T> resultOrError) {
        assert resultOrError != null;
        result = CompletableFuture.completedFuture(resultOrError);
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

    /**
     * Creates a new operation that transforms the result value using the given
     * mapper function. If this operation fails, the mapped operation will also
     * fail with the same error.
     *
     * @param <R>
     *            the mapped result type
     * @param mapper
     *            the function to transform the result value, not
     *            <code>null</code>
     * @return a new operation with the mapped result, not <code>null</code>
     */
    public <R> SignalOperation<R> map(SerializableFunction<T, R> mapper) {
        SignalOperation<R> mapped = new SignalOperation<>();
        forwardMappedResult(mapped, mapper);
        return mapped;
    }

    /**
     * Forwards the result of this operation to another operation after applying
     * the mapper function. If this operation fails, the target operation will
     * also fail with the same error.
     *
     * @param <R>
     *            the mapped result type
     * @param target
     *            the operation to forward the result to, not <code>null</code>
     * @param mapper
     *            the function to transform the result value, not
     *            <code>null</code>
     */
    protected <R> void forwardMappedResult(SignalOperation<R> target,
            SerializableFunction<T, R> mapper) {
        result.thenAccept(resultOrError -> {
            if (resultOrError.successful()) {
                @Nullable
                T value = ((Result<T>) resultOrError).value();
                @SuppressWarnings("NullAway")
                R mapped = mapper.apply(value);
                target.result().complete(new Result<>(mapped));
            } else {
                target.result().complete(
                        new Error<>(((Error<?>) resultOrError).reason()));
            }
        });
    }
}
