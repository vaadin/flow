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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableTriConsumer;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract class for common methods used in pre-made transfer progress
 * handlers.
 *
 * @param <T>
 *            type of transfer event, e.g. {@link DownloadHandler}
 * @param <R>
 *            type of the subclass implementing this abstract class, needed for
 *            revealing a proper type when you chain the methods
 */
public abstract class TransferProgressAwareHandler<T, R extends TransferProgressAwareHandler>
        implements Serializable {

    private List<TransferProgressListener> listeners;

    /**
     * This method is used to get the transfer context from the transfer events
     * (e.g. {@link DownloadEvent}).
     *
     * @param transferEvent
     *            the transfer event
     * @return the transfer context
     */
    protected abstract TransferContext getTransferContext(T transferEvent);

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
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here asynchrously when the download or upload request is being
     * handled. This needs {@link com.vaadin.flow.component.page.Push} to be
     * enabled in the application to properly send the UI changes to client.
     * <p>
     * Custom download/upload handler implementations can change this method to
     * be public or use it in handler's constructor.
     *
     * @param listener
     *            progress listener to be added to this handler
     * @return a {@link Registration} object that can be used to remove the
     *         added listener
     */
    protected Registration addTransferProgressListener(
            TransferProgressListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        TransferProgressListener wrapper = new TransferProgressListenerWrapper(
                listener);
        return addTransferProgressListenerInternal(wrapper);
    }

    /**
     * Adds a listener to be notified when the transfer starts.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param startHandler
     *            the handler to be called when the transfer starts
     * @return this instance for method chaining
     */
    public R whenStart(SerializableRunnable startHandler) {
        Objects.requireNonNull(startHandler, "Start handler cannot be null");
        addTransferProgressListenerInternal(new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                context.getUI().access(() -> {
                    startHandler.run();
                });
            }
        });
        return (R) this;
    }

    /**
     * Adds a listener to be notified when the transfer starts that receives the
     * transfer context as input.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param startHandler
     *            the handler to be called when the transfer starts
     * @return this instance for method chaining
     */
    public R whenStart(SerializableConsumer<TransferContext> startHandler) {
        Objects.requireNonNull(startHandler, "Start handler cannot be null");
        addTransferProgressListenerInternal(new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                context.getUI().access(() -> {
                    startHandler.accept(context);
                });
            }
        });
        return (R) this;
    }

    /**
     * Adds a listener to be notified of transfer progress with giving the
     * transfer context object and with the given interval.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param progressHandler
     *            the handler to be called with the transfer context, current
     *            and total bytes
     * @param progressIntervalInBytes
     *            the interval in bytes for reporting progress
     * @return this instance for method chaining
     */
    public R onProgress(
            SerializableTriConsumer<TransferContext, Long, Long> progressHandler,
            long progressIntervalInBytes) {
        Objects.requireNonNull(progressHandler,
                "Progress handler cannot be null");
        addTransferProgressListenerInternal(new TransferProgressListener() {
            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                context.getUI().access(() -> {
                    progressHandler.accept(context, transferredBytes,
                            totalBytes);
                });
            }

            @Override
            public long progressReportInterval() {
                return progressIntervalInBytes;
            }
        });
        return (R) this;
    }

    /**
     * Adds a listener to be notified of transfer progress with giving the
     * transfer context object and with the default progress interval.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param progressHandler
     *            the handler to be called with the transfer context, current
     *            and total bytes
     * @param progressIntervalInBytes
     *            the interval in bytes for reporting progress
     * @return this instance for method chaining
     */
    public R onProgress(
            SerializableTriConsumer<TransferContext, Long, Long> progressHandler) {
        return onProgress(progressHandler,
                TransferProgressListener.DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES);
    }

    /**
     * Adds a listener to be notified of transfer progress with the given
     * interval.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @param progressIntervalInBytes
     *            the interval in bytes for reporting progress
     * @return this instance for method chaining
     */
    public R onProgress(SerializableBiConsumer<Long, Long> progressHandler,
            long progressIntervalInBytes) {
        Objects.requireNonNull(progressHandler,
                "Progress handler cannot be null");
        addTransferProgressListenerInternal(new TransferProgressListener() {
            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                context.getUI().access(() -> {
                    progressHandler.accept(transferredBytes, totalBytes);
                });
            }

            @Override
            public long progressReportInterval() {
                return progressIntervalInBytes;
            }
        });
        return (R) this;
    }

    /**
     * Adds a listener to be notified of transfer progress with a default
     * interval.
     * <p>
     * The first long parameter is the current number of bytes transferred, and
     * the second is the total number of bytes.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     * <p>
     * The default progress report internal is <code>65536</code> bytes. To
     * change it, use {@link #onProgress(SerializableBiConsumer, long)}.
     *
     * @param progressHandler
     *            the handler to be called with the current and total bytes
     * @return this instance for method chaining
     */
    public R onProgress(SerializableBiConsumer<Long, Long> progressHandler) {
        return onProgress(progressHandler,
                TransferProgressListener.DEFAULT_PROGRESS_REPORT_INTERVAL_IN_BYTES);
    }

    /**
     * Adds a listener to be notified when the transfer is completed
     * successfully or with an error.
     * <p>
     * Gives a <code>Boolean</code> indicating whether the transfer was
     * completed successfully (true) or not (false).
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param completeOrTerminateHandler
     *            the handler to be called when the transfer is completed
     * @return this instance for method chaining
     */
    public R whenComplete(
            SerializableConsumer<Boolean> completeOrTerminateHandler) {
        Objects.requireNonNull(completeOrTerminateHandler,
                "Complete or terminate handler cannot be null");
        addTransferProgressListenerInternal(new TransferProgressListener() {
            @Override
            public void onError(TransferContext context, IOException reason) {
                context.getUI().access(() -> {
                    completeOrTerminateHandler.accept(false);
                });
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                context.getUI().access(() -> {
                    completeOrTerminateHandler.accept(true);
                });
            }
        });
        return (R) this;
    }

    /**
     * Adds a listener to be notified when the transfer is completed
     * successfully or with an error with the trasfer context object given as an
     * input.
     * <p>
     * Gives a <code>Boolean</code> indicating whether the transfer was
     * completed successfully (true) or not (false) and transfer context to
     * obtain more meta-data.
     * <p>
     * The call of the given callback is wrapped by the
     * {@link com.vaadin.flow.component.UI#access(Command)} to send UI changes
     * defined here when the download or upload request is being handled. This
     * needs {@link com.vaadin.flow.component.page.Push} to be enabled in the
     * application to properly send the UI changes to client.
     *
     * @param completeOrTerminateHandler
     *            the handler to be called when the transfer is completed
     * @return this instance for method chaining
     */
    public R whenComplete(
            SerializableBiConsumer<TransferContext, Boolean> completeOrTerminateHandler) {
        Objects.requireNonNull(completeOrTerminateHandler,
                "Complete or terminate handler cannot be null");
        addTransferProgressListenerInternal(new TransferProgressListener() {
            @Override
            public void onError(TransferContext context, IOException reason) {
                context.getUI().access(() -> {
                    completeOrTerminateHandler.accept(context, false);
                });
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                context.getUI().access(() -> {
                    completeOrTerminateHandler.accept(context, true);
                });
            }
        });
        return (R) this;
    }

    /**
     * Get the listeners that are registered to this handler.
     * <p>
     * For the custom data transfer implementation, one may need to notify
     * listeners manualy. This method can be used like
     * <code>getListeners().forEach(listener -> listener.onStart(getTransferContext(event)))</code>.
     * <p>
     * The listeners are kept in order of registration.
     *
     * @return a list of listeners that are registered to this handler
     */
    protected List<TransferProgressListener> getListeners() {
        return listeners == null ? Collections.emptyList()
                : Collections.unmodifiableList(listeners);
    }

    /**
     * Notifies all registered listeners about an error that occurred during a
     * data transfer operation.
     * <p>
     * Custom download/upload handler implementations can use this method to
     * notify listeners in the catch block, e.g.:
     *
     * <pre>
     * try () {
     *     // handler download/upload request
     * } catch (IOException ioe) {
     *     // process the error
     *     notifyError(event, ioe);
     *     throw ioe;
     * }
     * </pre>
     *
     *
     * @param transferEvent
     *            the meta-data associated with the operation where the error
     *            occurred
     * @param ioe
     *            the exception that describes the error
     */
    protected void notifyError(T transferEvent, IOException ioe) {
        TransferContext transferContext = getTransferContext(transferEvent);
        getListeners()
                .forEach(listener -> listener.onError(transferContext, ioe));
    }

    private Registration addTransferProgressListenerInternal(
            TransferProgressListener listener) {
        if (listeners == null) {
            // four listeners added with shortcuts is a good default size
            listeners = new ArrayList<>(4);
        }
        return Registration.addAndRemove(listeners, listener);
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
        public void onProgress(TransferContext context, long transferredBytes,
                long totalBytes) {
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
