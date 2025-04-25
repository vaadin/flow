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

public abstract class TransferProgressAwareHandler<T extends TransferRequest>
        implements TransferProgressAware {

    private Collection<TransferProgressListener> listeners;

    protected final void handleTransferProcessAwareRequest(T transferRequest) {
        Collection<TransferProgressListener> listeners = getListeners();
        listeners.forEach(listener -> listener.onStart(transferRequest));
        handleTransferRequest(transferRequest);
    }

    /**
     * Method that is called when the client wants to download from the url
     * stored for this specific handler registration.
     *
     * @param transferRequest
     *            transferRequest containing the necessary data for writing the
     *            response
     */
    protected abstract void handleTransferRequest(T transferRequest);

    public Registration addTransferProgressListener(
            TransferProgressListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        return Registration.addAndRemove(listeners, listener);
    }

    @Override
    public TransferProgressAwareHandler whenStart(
            SerializableRunnable startHandler) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onStart(TransferRequest context) {
                startHandler.run();
            }
        });
        return this;
    }

    @Override
    public TransferProgressAwareHandler onProgress(
            SerializableBiConsumer<Long, Long> progressHandler,
            long progressIntervalInBytes) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onProgress(TransferRequest request,
                    long transferredBytes, long totalBytes) {
                progressHandler.accept(transferredBytes, totalBytes);
            }

            @Override
            public long progressReportInterval() {
                return progressIntervalInBytes;
            }
        });
        return this;
    }

    @Override
    public TransferProgressAwareHandler whenComplete(
            SerializableConsumer<Long> completeHandler) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onComplete(TransferRequest request,
                    long transferredBytes) {
                completeHandler.accept(transferredBytes);
            }
        });
        return this;
    }

    @Override
    public TransferProgressAware onError(SerializableConsumer<IOException> errorHandler) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onError(TransferRequest request, IOException reason) {
                errorHandler.accept(reason);
            }
        });
        return this;
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

    void notifyError(T transferRequest, IOException ioe) {
        getListeners()
                .forEach(listener -> listener.onError(transferRequest, ioe));
    }
}
