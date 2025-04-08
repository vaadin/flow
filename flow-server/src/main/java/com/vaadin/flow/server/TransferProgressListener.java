package com.vaadin.flow.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import com.vaadin.flow.server.streams.TransferRequest;

/**
 * Interface for listening to transfer progress events.
 * <p>
 * Implementations of this interface can be used to monitor the progress of file
 * transfers, such as downloads or uploads.
 *
 * @since 24.8
 */
public interface TransferProgressListener extends Serializable {
    /**
     * Default data transfer progress report interval in bytes.
     * <p>
     * Chosen as a reasonable default for file sizes > 1 MB - 10-20% of the
     * total size.
     */
    long DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES = 65536;

    /**
     * Called when the data transfer is started.
     *
     * @param request
     *            the request of the transfer
     */
    default void onStart(TransferRequest request) {
        // Default implementation does nothing
    }

    /**
     * Called periodically during the transfer to report progress.
     *
     * @param request
     *            the request of the transfer
     * @param transferredBytes
     *            the number of bytes transferred so far
     * @param totalBytes
     *            the total number of bytes to be transferred
     */
    default void onProgress(TransferRequest request, long transferredBytes,
            long totalBytes) {
        // Default implementation does nothing
    }

    /**
     * Called when the transfer is terminated by user.
     *
     * @param request
     *            the request of the transfer
     */
    default void onTerminate(TransferRequest request) {
        // Default implementation does nothing
    }

    /**
     * Called when the transfer is failed.
     *
     * @param request
     *            the request of the transfer
     * @param reason
     *            the reason for termination
     */
    default void onFailure(TransferRequest request, Throwable reason) {
        // Default implementation does nothing
    }

    /**
     * Called when the transfer is started.
     *
     * @param request
     *            the request of the transfer
     */
    default void onComplete(TransferRequest request, long transferredBytes) {
        // Default implementation does nothing
    }

    /**
     * Returns the interval in milliseconds for reporting progress.
     * <p>
     * <code>-1</code> to not report progress.
     *
     * @return the interval in milliseconds
     */
    default long progressReportInterval() {
        return DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES;
    }
}
