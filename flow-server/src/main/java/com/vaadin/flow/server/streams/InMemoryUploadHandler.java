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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.TransferProgressListener;

/**
 * Upload handler for storing the upload in-memory. Data is returned as a
 * {@code byte[]} to the given successHandler.
 *
 * @since 24.8
 */
public class InMemoryUploadHandler
        extends TransferProgressAwareHandler<UploadEvent, InMemoryUploadHandler>
        implements UploadHandler {
    private final SerializableBiConsumer<UploadMetadata, byte[]> successHandler;

    public InMemoryUploadHandler(
            SerializableBiConsumer<UploadMetadata, byte[]> successHandler) {
        this.successHandler = successHandler;
    }

    @Override
    public void handleUploadRequest(UploadEvent event) {
        byte[] data;
        try {
            try (InputStream inputStream = event.getInputStream();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();) {
                TransferProgressListener.transfer(inputStream, outputStream,
                        getTransferContext(event), getListeners());
                data = outputStream.toByteArray();
            }
        } catch (IOException e) {
            notifyError(event, e);
            throw new UncheckedIOException(e);
        }
        successHandler.accept(new UploadMetadata(event.getFileName(),
                event.getContentType(), event.getFileSize()), data);
    }

    @Override
    protected TransferContext getTransferContext(UploadEvent transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.getSession(),
                transferEvent.getFileName(),
                transferEvent.getOwningComponent().getElement(),
                transferEvent.getFileSize());
    }
}
