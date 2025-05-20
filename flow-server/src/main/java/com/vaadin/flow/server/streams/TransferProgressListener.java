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

package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.server.VaadinSession;

/**
 * Interface for listening to transfer progress events.
 * <p>
 * Implementations of this interface can be used to monitor the progress of file
 * transfers, such as downloads or uploads.
 * <p>
 * It uses
 * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
 * to send UI changes from progress listeners when the download or upload
 * request is being handled. Thus, it needs
 * {@link com.vaadin.flow.component.page.Push} to be enabled in the application.
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
     * Default buffer size for reading data from the input stream.
     * <p>
     * Follows the default buffer size of the Java
     * {@link InputStream#transferTo(OutputStream)}.
     */
    int DEFAULT_BUFFER_SIZE = 16384;

    /**
     * Called when the data transfer is started.
     * <p>
     * The call of this method is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * to send UI changes defined here when the download or upload request is
     * being handled. Thus, no need to call
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * in the implementation of this method. This needs
     * {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param context
     *            the context of the transfer
     */
    default void onStart(TransferContext context) {
        // Default implementation does nothing
    }

    /**
     * Called periodically during the transfer to report progress.
     * <p>
     * The call of this method is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * to send UI changes defined here asynchronously when the download or
     * upload request is being handled. This needs
     * {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     * <p>
     * The default progress report internal is <code>65536</code> bytes. To
     * change it, override {@link #progressReportInterval()}.
     *
     * @param context
     *            the context of the transfer
     * @param transferredBytes
     *            the number of bytes transferred so far
     * @param totalBytes
     *            the total number of bytes to be transferred or <code>-1</code>
     *            if total number is unknown in advance, e.g. when reading from
     *            an input stream
     */
    default void onProgress(TransferContext context, long transferredBytes,
            long totalBytes) {
        // Default implementation does nothing
    }

    /**
     * Called when the transfer is failed.
     * <p>
     * The call of this method is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * to send UI changes defined here when the download or upload request is
     * being handled. Thus, no need to call
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * in the implementation of this method. This needs
     * {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param context
     *            the context of the transfer
     * @param reason
     *            the origin I/O exception that terminated the transfer
     */
    default void onError(TransferContext context, IOException reason) {
        // Default implementation does nothing
    }

    /**
     * Called when the transfer is started.
     * <p>
     * The call of this method is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * to send UI changes defined here when the download or upload request is
     * being handled. Thus, no need to call
     * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)}
     * in the implementation of this method. This needs
     * {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param context
     *            the context of the transfer
     */
    default void onComplete(TransferContext context, long transferredBytes) {
        // Default implementation does nothing
    }

    /**
     * Returns the interval in bytes for reporting progress.
     * <p>
     * <code>-1</code> to not report progress.
     * <p>
     * The default value is <code>65536</code> bytes.
     *
     * @return the interval in bytes
     */
    default long progressReportInterval() {
        return DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES;
    }

    /**
     * Transfers data from the given input stream to the output stream while
     * notifying the progress to the given listeners.
     *
     * @param inputStream
     *            the input stream to read from
     * @param outputStream
     *            the output stream to write to
     * @param transferContext
     *            the transfer request containing metadata about the transfer
     * @param listeners
     *            collection of listeners to notify about progress
     * @return the number of bytes transferred
     * @throws IOException
     *             if an I/O error occurs during the transfer
     */
    static long transfer(InputStream inputStream, OutputStream outputStream,
            TransferContext transferContext,
            Collection<TransferProgressListener> listeners) throws IOException {
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        Objects.requireNonNull(outputStream, "OutputStream cannot be null");
        Objects.requireNonNull(transferContext,
                "TransferRequest cannot be null");
        Objects.requireNonNull(listeners,
                "TransferProgressListener cannot be null");
        listeners.forEach(listener -> listener.onStart(transferContext));
        long transferred = 0;
        Map<TransferProgressListener, Long> lastNotified = new HashMap<>(
                listeners.size());
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            outputStream.write(buffer, 0, read);
            if (transferred < Long.MAX_VALUE) {
                try {
                    transferred = Math.addExact(transferred, read);
                } catch (ArithmeticException ignore) {
                    transferred = Long.MAX_VALUE;
                }
                for (TransferProgressListener listener : listeners) {
                    Long lastNotifiedLong = lastNotified.getOrDefault(listener,
                            0L);
                    long progressReportInterval = listener
                            .progressReportInterval();
                    if (progressReportInterval > -1 && transferred
                            - lastNotifiedLong >= progressReportInterval) {
                        long finalTransferred = transferred;
                        listener.onProgress(transferContext, finalTransferred,
                                transferContext.contentLength());
                        lastNotified.put(listener, transferred);
                    }
                }
            }
        }
        long finalTransferred = transferred;
        listeners.forEach(listener -> listener.onComplete(transferContext,
                finalTransferred));
        return transferred;
    }
}
