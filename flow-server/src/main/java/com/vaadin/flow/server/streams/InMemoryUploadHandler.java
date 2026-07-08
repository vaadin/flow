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
            runMetadataValidators(event);
            if (!event.isRejected()) {
                try (InputStream raw = event.getInputStream();
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    InputStream in = applyHeaderValidators(event, raw);
                    if (!event.isRejected()) {
                        TransferUtil.transfer(in, outputStream,
                                getTransferContext(event), getListeners());
                        data = outputStream.toByteArray();
                    }
                }
            }
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
        if (event.isRejected()) {
            // A validator rejected the upload; report it as a terminal error so
            // progress listeners get a signal.
            notifyError(event,
                    new UploadRejectedException(event.getRejectionMessage()));
        }
        final byte[] delivered = data;
        event.getUI().access(() -> {
            // A validator may have rejected the upload while it was received;
            // the accumulated data must not be delivered in that case.
            if (event.isRejected()) {
                return;
            }
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
}
