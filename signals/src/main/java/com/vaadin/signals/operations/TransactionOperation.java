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
