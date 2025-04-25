package com.vaadin.flow.server;

import java.io.IOException;
import java.io.Serializable;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Interface for handling transfer progress events.
 *
 * @since 24.8
 */
public interface TransferProgressAware extends Serializable {

    /**
     * Adds a listener to be notified when the transfer starts.
     *
     * @param startHandler
     *            the handler to be called when the transfer starts
     * @return this instance for method chaining
     */
    TransferProgressAware whenStart(SerializableRunnable startHandler);

    /**
     * Adds a listener to be notified of transfer progress.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @param progressIntervalInBytes
     *            the interval in bytes for reporting progress
     * @return this instance for method chaining
     */
    TransferProgressAware onProgress(
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
    default TransferProgressAware onProgress(
            SerializableBiConsumer<Long, Long> progressHandler) {
        return onProgress(progressHandler,
                TransferProgressListener.DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES);
    }

    /**
     * Adds a listener to be notified when the transfer fails.
     *
     * @param reason
     *            the origin I/O exception that terminated the transfer
     * @return this instance for method chaining
     */
    TransferProgressAware onError(SerializableConsumer<IOException> reason);

    /**
     * Adds a listener to be notified when the transfer is completed.
     *
     * @param completeHandler
     *            the handler to be called when the transfer is completed
     * @return this instance for method chaining
     */
    TransferProgressAware whenComplete(
            SerializableConsumer<Long> completeHandler);

    /**
     * Unsubscribes from progress updates.
     */
    void unsubscribe();
}
