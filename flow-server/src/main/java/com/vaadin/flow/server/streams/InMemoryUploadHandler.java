/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.vaadin.flow.function.SerializableBiConsumer;

/**
 * Upload handler for storing the upload in-memory. Data is returned as a
 * {@code byte[]} to the given successHandler.
 *
 * @since 24.8
 */
public class InMemoryUploadHandler
        extends TransferProgressAwareHandler<UploadEvent, InMemoryUploadHandler>
        implements UploadHandler {
    private final InMemoryUploadCallback successCallback;

    public InMemoryUploadHandler(InMemoryUploadCallback successCallback) {
        this.successCallback = successCallback;
    }

    @Override
    public void handleUploadRequest(UploadEvent event) throws IOException {
        setTransferUI(event.getUI());
        byte[] data;
        try {
            try (InputStream inputStream = event.getInputStream();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                TransferUtil.transfer(inputStream, outputStream,
                        getTransferContext(event), getListeners());
                data = outputStream.toByteArray();
            }
        } catch (IOException e) {
            notifyError(event, e);
            throw e;
        }
        event.getUI().access(() -> {
            try {
                successCallback.complete(
                        new UploadMetadata(event.getFileName(),
                                event.getContentType(), event.getFileSize()),
                        data);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Error in memory upload callback", e);
            }
        });
    }

    @Override
    protected TransferContext getTransferContext(UploadEvent transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.getSession(),
                transferEvent.getFileName(), transferEvent.getOwningElement(),
                transferEvent.getFileSize());
    }
}
