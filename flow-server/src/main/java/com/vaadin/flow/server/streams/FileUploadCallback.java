/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.streams;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Callback interface for handling file uploads in
 * {@link AbstractFileUploadHandler}.
 *
 * This interface is used to process the uploaded file and its metadata after
 * the upload is complete.
 *
 * The method invocation may throw an {@link IOException} to handle cases where
 * hadnling a file fails.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface FileUploadCallback extends Serializable {

    /**
     * Applies the given callback once the file upload is complete.
     *
     * @param metadata
     *            the upload metadata containing relevant information about the
     *            upload
     * @param file
     *            the file to which the data will be written
     * @throws IOException
     *             if an I/O error occurs in the callback
     */
    void complete(UploadMetadata metadata, File file) throws IOException;
}
