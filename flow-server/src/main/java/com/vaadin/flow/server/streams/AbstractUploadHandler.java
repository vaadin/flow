/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for the pre-made upload handlers, adding support for synchronous
 * {@link UploadValidator}s on top of the transfer progress handling.
 * <p>
 * Validators are held here, rather than on
 * {@link TransferProgressAwareHandler}, so that they apply only to uploads and
 * never to downloads.
 *
 * @param <R>
 *            type of the subclass implementing this class, for method chaining
 */
public abstract class AbstractUploadHandler<R extends AbstractUploadHandler>
        extends TransferProgressAwareHandler<UploadEvent, R>
        implements UploadHandler {

    private List<UploadValidator> validators;

    /**
     * Adds a validator that can reject the upload while it is being received.
     * <p>
     * The validator's phase methods are invoked synchronously on the request
     * thread; calling {@link UploadEvent#reject(String)} from any of them
     * aborts the upload and prevents the success callback from being invoked.
     * Validators are invoked in the order they are added; the first rejection
     * stops further reading.
     *
     * @param validator
     *            the validator to add, not {@code null}
     * @return this instance for method chaining
     */
    public R withValidator(UploadValidator validator) {
        Objects.requireNonNull(validator, "Validator cannot be null");
        if (validators == null) {
            validators = new ArrayList<>(2);
        }
        validators.add(validator);
        return (R) this;
    }

    /**
     * Adds a validator that inspects only the upload metadata, before any data
     * is read.
     *
     * @param callback
     *            the metadata validation callback, not {@code null}
     * @return this instance for method chaining
     * @see UploadValidator#validateMetadata(UploadEvent)
     */
    public R validateMetadata(UploadMetadataCallback callback) {
        Objects.requireNonNull(callback,
                "Metadata validation callback cannot be null");
        return withValidator(metadataValidator(callback));
    }

    /**
     * Adds a validator that inspects the first {@code maxBytes} bytes of the
     * upload, for example a file-signature ("magic byte") or MIME-sniffing
     * check. The validator is invoked once with the assembled leading bytes and
     * can reject the upload after only those bytes have been read.
     *
     * @param maxBytes
     *            the number of leading bytes to inspect, must be positive
     * @param callback
     *            the header validation callback, not {@code null}
     * @return this instance for method chaining
     * @see UploadValidator#validateHeader(UploadEvent, ByteBuffer)
     */
    public R validateHeader(int maxBytes, UploadHeaderCallback callback) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        Objects.requireNonNull(callback,
                "Header validation callback cannot be null");
        return withValidator(headerValidator(callback, maxBytes));
    }

    /**
     * Adds a validator that inspects the fully received upload, before it is
     * delivered, for example an antivirus scan.
     *
     * @param callback
     *            the complete validation callback, not {@code null}
     * @return this instance for method chaining
     * @see UploadValidator#validateComplete(UploadEvent, UploadContent)
     */
    public R validateComplete(UploadCompleteCallback callback) {
        Objects.requireNonNull(callback,
                "Complete validation callback cannot be null");
        return withValidator(completeValidator(callback));
    }

    /**
     * Runs the metadata phase of the registered validators, before any data is
     * read. Stops at the first rejection.
     *
     * @param event
     *            the upload being handled
     * @throws IOException
     *             if a validator fails
     */
    protected void runMetadataValidators(UploadEvent event) throws IOException {
        if (validators == null) {
            return;
        }
        for (UploadValidator validator : validators) {
            validator.validateMetadata(event);
            if (event.isRejected()) {
                return;
            }
        }
    }

    /**
     * Runs the header phase of the registered validators: reads the leading
     * bytes (up to the largest requested {@link UploadValidator#headerSize()}),
     * gives each header validator a read-only view of its first
     * {@code headerSize()} bytes, and returns a stream that replays those bytes
     * followed by the rest of {@code in}. Returns {@code in} unchanged when no
     * header validation is requested or once the upload is rejected. Stops at
     * the first rejection.
     *
     * @param event
     *            the upload being handled
     * @param in
     *            the upload input stream
     * @return the stream to transfer, with the consumed header spliced back in
     * @throws IOException
     *             if reading the header or a validator fails
     */
    protected InputStream applyHeaderValidators(UploadEvent event,
            InputStream in) throws IOException {
        if (validators == null) {
            return in;
        }
        int max = 0;
        for (UploadValidator validator : validators) {
            max = Math.max(max, Math.max(0, validator.headerSize()));
        }
        if (max == 0) {
            return in;
        }
        byte[] header = in.readNBytes(max);
        for (UploadValidator validator : validators) {
            int size = validator.headerSize();
            if (size > 0) {
                validator.validateHeader(event,
                        ByteBuffer
                                .wrap(header, 0, Math.min(size, header.length))
                                .asReadOnlyBuffer());
                if (event.isRejected()) {
                    return in;
                }
            }
        }
        return new SequenceInputStream(new ByteArrayInputStream(header), in);
    }

    /**
     * Runs the complete phase of the registered validators against the fully
     * received content. Stops at the first rejection.
     *
     * @param event
     *            the upload being handled
     * @param content
     *            handle to the received content
     * @throws IOException
     *             if a validator fails
     */
    protected void runCompleteValidators(UploadEvent event,
            UploadContent content) throws IOException {
        if (validators == null) {
            return;
        }
        for (UploadValidator validator : validators) {
            validator.validateComplete(event, content);
            if (event.isRejected()) {
                return;
            }
        }
    }

    /**
     * Whether any validators are registered. The handlers use this to skip the
     * complete-validation phase (and building an {@link UploadContent})
     * entirely when there is nothing to validate.
     *
     * @return {@code true} if at least one validator is registered
     */
    protected boolean hasValidators() {
        return validators != null && !validators.isEmpty();
    }

    @Override
    protected TransferContext getTransferContext(UploadEvent event) {
        return new TransferContext(event.getRequest(), event.getResponse(),
                event.getSession(), event.getFileName(),
                event.getOwningElement(), event.getFileSize());
    }

    private static UploadValidator metadataValidator(
            UploadMetadataCallback callback) {
        return new UploadValidator() {
            @Override
            public void validateMetadata(UploadEvent event) throws IOException {
                callback.validate(event);
            }
        };
    }

    private static UploadValidator headerValidator(
            UploadHeaderCallback callback, int maxBytes) {
        return new UploadValidator() {
            @Override
            public void validateHeader(UploadEvent event, ByteBuffer header)
                    throws IOException {
                callback.validate(event, header);
            }

            @Override
            public int headerSize() {
                return maxBytes;
            }
        };
    }

    private static UploadValidator completeValidator(
            UploadCompleteCallback callback) {
        return new UploadValidator() {
            @Override
            public void validateComplete(UploadEvent event,
                    UploadContent content) throws IOException {
                callback.validate(event, content);
            }
        };
    }
}
