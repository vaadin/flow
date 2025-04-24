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

    private static final int DEFAULT_BUFFER_SIZE = 16384;

    private Collection<TransferProgressListener> listeners;

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

    /**
     * Transfers data from the given input stream to the output stream while
     * notifying the progress to the given listeners.
     *
     * @param inputStream
     *            the input stream to read from
     * @param outputStream
     *            the output stream to write to
     * @param transferRequest
     *            the transfer request containing metadata about the transfer
     * @param listeners
     *            collection of listeners to notify about progress
     * @return the number of bytes transferred
     * @throws IOException
     *             if an I/O error occurs during the transfer
     */
    long transfer(InputStream inputStream, OutputStream outputStream,
            TransferRequest transferRequest,
            Collection<TransferProgressListener> listeners) throws IOException {
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        Objects.requireNonNull(outputStream, "OutputStream cannot be null");
        Objects.requireNonNull(transferRequest,
                "TransferRequest cannot be null");
        Objects.requireNonNull(listeners,
                "TransferProgressListener cannot be null");
        if (terminated) {
            return 0;
        }
        long transferred = 0;
        long lastNotified = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = read(transferRequest.getSession(), inputStream,
                buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
            if (transferred < Long.MAX_VALUE) {
                try {
                    transferred = Math.addExact(transferred, read);
                } catch (ArithmeticException ignore) {
                    transferred = Long.MAX_VALUE;
                }
                if (transferred - lastNotified >= transferRequest
                        .getTransferInterval()) {
                    for (TransferProgressListener listener : listeners) {
                        listener.onProgress(transferRequest, transferred,
                                transferRequest.getSize());
                    }
                    lastNotified = transferred;
                }
            }
            if (terminated) {
                getListeners().forEach(
                        listener -> listener.onTerminate(transferRequest));
                break;
            }
        }
        if (!terminated) {
            long finalTransferred = transferred;
            getListeners().forEach(listener -> listener
                    .onComplete(transferRequest, finalTransferred));
        }
        return transferred;
    }

    /**
     * Read buffer amount of bytes from the input stream.
     *
     * @param session
     *            vaadin session in use
     * @param source
     *            input stream source
     * @param buffer
     *            byte buffer to read into
     * @return amount of bytes read into buffer
     * @throws IOException
     *             If the first byte cannot be read for any reason other than
     *             the end of the file, if the input stream has been closed, or
     *             if some other I/O error occurs.
     */
    static int read(VaadinSession session, InputStream source, byte[] buffer)
            throws IOException {
        session.lock();
        try {
            return source.read(buffer, 0, DEFAULT_BUFFER_SIZE);
        } finally {
            session.unlock();
        }
    }

    @Override
    public void terminate() {
        terminated = true;
    }
}
