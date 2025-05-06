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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.DownloadEvent;
import com.vaadin.flow.server.TransferProgressAware;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract class for common methods used in pre-made transfer progress
 * handlers.
 *
 * @param <T>
 *            type of transfer event, e.g.
 *            {@link com.vaadin.flow.server.DownloadHandler}
 */
public abstract class TransferProgressAwareHandler<R, T extends TransferProgressAware<T>>
        implements TransferProgressAware<T> {

    private Map<TransferProgressEventType, List<SerializableConsumer<TransferContext>>> listeners;

    /**
     * This method is used to get the transfer context from the transfer events
     * (e.g. {@link DownloadEvent}).
     *
     * @param transferEvent
     *            the transfer event
     * @return the transfer context
     */
    protected abstract TransferContext getTransferContext(R transferEvent);

    /**
     * Adds a listener to be notified of data transfer progress events, such as:
     * <ul>
     * <li>{@link TransferProgressListener#onStart(TransferContext)}</li>
     * <li>{@link TransferProgressListener#onProgress(TransferContext, long, long)}</li>
     * <li>{@link TransferProgressListener#onError(TransferContext, IOException)}</li>
     * <li>{@link TransferProgressListener#onComplete(TransferContext, long)}</li>
     * </ul>
     * <p>
     * The calls of the given listener's methods are wrapped by the
     * {@link com.vaadin.flow.component.UI#access} to send UI changes defined
     * here when the download or upload request is being handled. Thus, no need
     * to call {@link com.vaadin.flow.component.UI#access} in the implementation
     * of the given listener. This needs
     * {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param listener
     *            progress listener to be added to this handler
     * @return a {@link Registration} object that can be used to remove the
     *         added listener
     */
    public Registration addTransferProgressListener(
            TransferProgressListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        if (listeners == null) {
            listeners = new HashMap<>(4);
        }
        TransferProgressListener wrapper = new TransferProgressListenerWrapper(
                listener);
        SerializableConsumer<TransferContext> onStartListener = wrapper::onStart;
        listeners.computeIfAbsent(TransferProgressEventType.START,
                event -> new ArrayList<>()).add(onStartListener);

        SerializableConsumer<TransferContext> onProgressListener = wrapper::onProgress;
        listeners.computeIfAbsent(TransferProgressEventType.PROGRESS,
                event -> new ArrayList<>()).add(onProgressListener);

        SerializableConsumer<TransferContext> onErrorListener = wrapper::onError;
        listeners.computeIfAbsent(TransferProgressEventType.ERROR,
                event -> new ArrayList<>()).add(onErrorListener);

        SerializableConsumer<TransferContext> onCompleteListener = wrapper::onComplete;
        listeners.computeIfAbsent(TransferProgressEventType.COMPLETE,
                event -> new ArrayList<>()).add(onCompleteListener);

        return Registration.combine(
                () -> listeners.getOrDefault(
                        TransferProgressEventType.START, Collections.emptyList())
                        .remove(onStartListener),
                () -> listeners.getOrDefault(
                        TransferProgressEventType.PROGRESS,
                        Collections.emptyList()).remove(onProgressListener),
                () -> listeners.getOrDefault(
                        TransferProgressEventType.ERROR,
                        Collections.emptyList()).remove(onErrorListener),
                () -> listeners.getOrDefault(
                        TransferProgressEventType.COMPLETE,
                        Collections.emptyList()).remove(onCompleteListener));
    }

    @Override
    public T whenStart(SerializableRunnable startHandler) {
        listeners.computeIfAbsent(TransferProgressEventType.START,
                event -> new ArrayList<>()).add(context -> context.getUI().access(() -> startHandler.run())
        );
        return (T) this;
    }

    @Override
    public T onProgress(SerializableBiConsumer<Long, Long> progressHandler,
            long progressIntervalInBytes) {
        listeners.computeIfAbsent(TransferProgressEventType.PROGRESS,
                event -> new ArrayList<>()).add(context -> context.getUI().access(() -> progressHandler.accept(context.transferredBytes(), context.totalBytes()))
        );
        return (T) this;
    }

    @Override
    public T whenComplete(
            SerializableConsumer<Boolean> completeOrTerminateHandler) {
        listeners.computeIfAbsent(TransferProgressEventType.COMPLETE,
                event -> new ArrayList<>()).add(context -> context.getUI().access(() -> {
                    if (context.reason() != null) {
                        completeOrTerminateHandler.accept(false);
                    } else {
                        completeOrTerminateHandler.accept(true);
                    }
                })
        );
        return (T) this;
    }

    @Override
    public void unsubscribeFromTransferProgress() {
        if (listeners != null) {
            listeners.clear();
            listeners = null;
        }
    }

    protected List<SerializableConsumer<TransferContext>> getListeners(TransferProgressEventType eventType) {
        return TransferProgressListener.getListeners(getListeners(), eventType);
    }

    Map<TransferProgressEventType, List<SerializableConsumer<TransferContext>>> getListeners() {
        return listeners == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(listeners);
    }

    void notifyError(R transferEvent, IOException ioe) {
        TransferContext transferContext = getTransferContext(transferEvent);
        getListeners(TransferProgressEventType.ERROR)
                .forEach(listener -> listener.accept(transferContext));
    }

    /**
     * A wrapper for {@link TransferProgressListener} that ensures that UI
     * updates in transfer progress listeners are pushed to client
     * asynchronously.
     */
    private final class TransferProgressListenerWrapper
            implements TransferProgressListener {
        private TransferProgressListener delegate;

        public TransferProgressListenerWrapper(
                TransferProgressListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onStart(TransferContext context) {
            context.getUI().access(() -> {
                delegate.onStart(context);
            });
        }

        @Override
        public void onProgress(TransferContext context, long transferredBytes, long totalBytes) {
            context.getUI().access(() -> {
                delegate.onProgress(context, transferredBytes, totalBytes);
            });
        }

        @Override
        public void onError(TransferContext context, IOException reason) {
            context.getUI().access(() -> {
                delegate.onError(context, reason);
            });
        }

        @Override
        public void onComplete(TransferContext context, long transferredBytes) {
            context.getUI().access(() -> {
                delegate.onComplete(context, transferredBytes);
            });
        }

        @Override
        public long progressReportInterval() {
            return delegate.progressReportInterval();
        }
    }
}
