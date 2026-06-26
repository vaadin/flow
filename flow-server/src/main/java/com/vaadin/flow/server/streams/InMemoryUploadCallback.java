/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.Serializable;

/**
 * Callback interface for handling in-memory uploads in
 * {@link InMemoryUploadHandler}.
 *
 * This interface is used to process the upload metadata after the upload is
 * complete. The method invocation may throw an {@link IOException} to handle
 * cases where processing the upload fails.
 *
 * @since 24.8
 */
public interface InMemoryUploadCallback extends Serializable {

    /**
     * Applies the given callback once the in-memory data upload is complete.
     *
     * @param metadata
     *            the upload metadata containing relevant information about the
     *            upload
     * @throws IOException
     *             if an I/O error occurs in the callback
     */
    void complete(UploadMetadata metadata, byte[] data) throws IOException;
}
