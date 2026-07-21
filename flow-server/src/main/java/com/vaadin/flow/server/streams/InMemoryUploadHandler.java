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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.vaadin.flow.server.communication.TransferUtil;

/**
 * Upload handler for storing the upload in-memory. Data is returned as a
 * {@code byte[]} to the given successHandler.
 *
 * @since 24.8
 */
public class InMemoryUploadHandler
        extends AbstractUploadHandler<InMemoryUploadHandler> {
    private final InMemoryUploadCallback successCallback;

    public InMemoryUploadHandler(InMemoryUploadCallback successCallback) {
        this.successCallback = successCallback;
    }

    @Override
    public void handleUploadRequest(UploadEvent event) throws IOException {
        setTransferUI(event.getUI());
        byte[] data = null;
        try {
            data = readContent(event);
        } catch (IOException e) {
            notifyError(event, e);
            throw e;
        }
        if (hasValidators() && data != null && !event.isRejected()) {
            // Complete phase runs after the transfer's onComplete has already
            // fired, so any failure here is reported via onError.
            try {
                runCompleteValidators(event, new ByteArrayUploadContent(data));
            } catch (IOException e) {
                notifyError(event, e);
                throw e;
            } catch (RuntimeException e) {
                notifyError(event, new IOException(e));
                throw e;
            }
        }
        // A validator may reject the upload during any phase (metadata, header
        // or complete); all of them converge here. Rejection is fully settled
        // by this point, since every validator has already run. The transfer's
        // own onComplete may already have fired, so the rejection is surfaced
        // as a terminal onError to give progress listeners a definitive signal,
        // and the accumulated data is never delivered.
        if (event.isRejected()) {
            notifyError(event,
                    new UploadRejectedException(event.getRejectionMessage()));
            return;
        }
        final byte[] delivered = data;
        event.getUI().access(() -> {
            try {
                successCallback.complete(
                        new UploadMetadata(event.getFileName(),
                                event.getContentType(), event.getFileSize()),
                        delivered);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Error in memory upload callback", e);
            }
        });
    }

    /**
     * Runs metadata and header validation and, if the upload survives both,
     * reads the full content into memory.
     *
     * @param event
     *            the upload being handled
     * @return the received bytes, or {@code null} if a validator rejected the
     *         upload before the transfer completed
     * @throws IOException
     *             if reading the content or a validator fails
     */
    private byte[] readContent(UploadEvent event) throws IOException {
        runMetadataValidators(event);
        if (event.isRejected()) {
            return null;
        }
        try (InputStream raw = event.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            InputStream in = applyHeaderValidators(event, raw);
            if (event.isRejected()) {
                return null;
            }
            TransferUtil.transfer(in, outputStream, getTransferContext(event),
                    getListeners());
            return outputStream.toByteArray();
        }
    }
}
