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
