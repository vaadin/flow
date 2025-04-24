package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Interface for handling transfer progress events.
 *
 * @since 24.8
 */
public interface TransferProgressAwareHandler extends Serializable {

    /**
     * Adds a listener to be notified when the transfer starts.
     *
     * @param startHandler
     *            the handler to be called when the transfer starts
     * @return this instance for method chaining
     */
    TransferProgressAwareHandler whenStart(SerializableRunnable startHandler);

    /**
     * Adds a listener to be notified of transfer progress.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @param progressIntervalInBytes
     *            the interval in bytes for reporting progress
     * @return this instance for method chaining
     */
    TransferProgressAwareHandler onProgress(
            SerializableBiConsumer<Long, Long> progressHandler,
            long progressIntervalInBytes);

    /**
     * Adds a listener to be notified of transfer progress with a default
     * interval.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @return this instance for method chaining
     */
    default TransferProgressAwareHandler onProgress(
            SerializableBiConsumer<Long, Long> progressHandler) {
        return onProgress(progressHandler,
                TransferProgressListener.DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES);
    }

    /**
     * Adds a listener to be notified when the transfer is completed.
     *
     * @param completeHandler
     *            the handler to be called when the transfer is completed
     * @return this instance for method chaining
     */
    TransferProgressAwareHandler whenComplete(
            SerializableBiConsumer<CompletionStatus, Long> completeHandler);

    /**
     * Unsubscribes from progress updates.
     * <p>
     * This method can be overridden to provide custom unsubscribe logic.
     */
    void unsubscribeFromProgress();

    void terminate();

    /**
     * Enum representing the completion status of a transfer.
     */
    public enum CompletionStatus {
        /**
         * The transfer was completed successfully.
         */
        COMPLETED,
        /**
         * The transfer was terminated by the user.
         */
        TERMINATED,
        /**
         * The transfer failed.
         */
        FAILED
    }
}
