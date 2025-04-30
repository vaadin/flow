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

package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Interface for handling transfer progress events.
 *
 * @since 24.8
 */
public interface TransferProgressAware<T extends TransferProgressAware<T>>
        extends Serializable {

    /**
     * Adds a listener to be notified when the transfer starts.
     *
     * @param startHandler
     *            the handler to be called when the transfer starts
     * @return this instance for method chaining
     */
    T whenStart(SerializableRunnable startHandler);

    /**
     * Adds a listener to be notified of transfer progress.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @param progressIntervalInBytes
     *            the interval in bytes for reporting progress
     * @return this instance for method chaining
     */
    T onProgress(SerializableBiConsumer<Long, Long> progressHandler,
            long progressIntervalInBytes);

    /**
     * Adds a listener to be notified of transfer progress with a default
     * interval.
     * <p>
     * The first long parameter is the current number of bytes transferred, and
     * the second is the total number of bytes.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @return this instance for method chaining
     */
    default T onProgress(SerializableBiConsumer<Long, Long> progressHandler) {
        return onProgress(progressHandler,
                TransferProgressListener.DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES);
    }

    /**
     * Adds a listener to be notified when the transfer is completed
     * successfully or with an error. Gives a <code>Boolean</code> indicating
     * whether the transfer was completed successfully (true) or not (false).
     *
     * @param completeHandler
     *            the handler to be called when the transfer is completed
     * @return this instance for method chaining
     */
    T whenComplete(SerializableConsumer<Boolean> completeOrTerminateHandler);

    /**
     * Unsubscribes from progress updates.
     */
    void unsubscribeFromTransferProgress();
}
