package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.TransferProgressAwareHandler;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

public abstract class AbstractTransferProgressAwareHandler<T extends TransferRequest>
        implements TransferProgressAwareHandler {

    private Collection<TransferProgressListener> listeners;

    // TODO: should not be shared within single session
    private boolean terminated = false;

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
    public AbstractTransferProgressAwareHandler whenStart(
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
    public AbstractTransferProgressAwareHandler onProgress(
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
    public AbstractTransferProgressAwareHandler whenComplete(
            SerializableBiConsumer<CompletionStatus, Long> completeHandler) {
        addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onComplete(TransferRequest request,
                    long transferredBytes) {
                completeHandler.accept(CompletionStatus.COMPLETED,
                        transferredBytes);
            }

            @Override
            public void onTerminate(TransferRequest request) {
                // TODO: Implement termination logic
                completeHandler.accept(CompletionStatus.TERMINATED, null);
            }

            @Override
            public void onFailure(TransferRequest request, Throwable reason) {
                completeHandler.accept(CompletionStatus.FAILED, null);
            }
        });
        return this;
    }

    @Override
    public void unsubscribeFromProgress() {
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
                .forEach(listener -> listener.onFailure(transferRequest, ioe));
    }

    @Override
    public void terminate() {
        terminated = true;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }
}
