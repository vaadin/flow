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
     */
    public static record Error<T>(String reason) implements ResultOrError<T> {
        @Override
        public boolean successful() {
            return false;
        }
    }

    private final CompletableFuture<ResultOrError<T>> result = new CompletableFuture<>();

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
