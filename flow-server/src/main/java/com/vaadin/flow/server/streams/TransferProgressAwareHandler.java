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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.TransferProgressAware;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract class for common methods used in pre-made transfer progress
 * handlers.
 *
 * @param <T>
 *            type of transfer event, e.g. {@link DownloadRequest}
 */
public abstract class TransferProgressAwareHandler<R, T extends TransferProgressAware<T>>
        implements TransferProgressAware<T> {

    private Collection<TransferProgressListener> listeners;

    /**
     * Method that is called when the client wants to download from the url
     * stored for this specific handler registration.
     *
     * @param transferEvent
     *            transferEvent containing the necessary data for writing the
     *            response
     */
    protected abstract void handleTransfer(R transferEvent);

    protected abstract TransferContext getTransferContext(R transferEvent);

    public Registration addTransferProgressListener(
            TransferProgressListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        return Registration.addAndRemove(listeners, listener);
    }

    @Override
    public T whenStart(SerializableRunnable startHandler) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                startHandler.run();
            }
        });
        return (T) this;
    }

    @Override
    public T onProgress(SerializableBiConsumer<Long, Long> progressHandler,
            long progressIntervalInBytes) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes) {
                progressHandler.accept(transferredBytes, transferredBytes);
            }

            @Override
            public long progressReportInterval() {
                return progressIntervalInBytes;
            }
        });
        return (T) this;
    }

    @Override
    public T whenComplete(
            SerializableConsumer<Boolean> completeOrTerminateHandler) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onError(TransferContext context, IOException reason) {
                completeOrTerminateHandler.accept(false);
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                completeOrTerminateHandler.accept(true);
            }
        });
        return (T) this;
    }

    @Override
    public void unsubscribe() {
        if (listeners != null) {
            listeners.clear();
            listeners = null;
        }
    }

    Collection<TransferProgressListener> getListeners() {
        return listeners == null ? Collections.emptyList()
                : new ArrayList<>(listeners);
    }

    void notifyError(R transferEvent, IOException ioe) {
        TransferContext transferContext = getTransferContext(transferEvent);
        getListeners()
                .forEach(listener -> listener.onError(transferContext, ioe));
    }
}
