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

    /**
     * Called when a file upload is rejected.
     * <p>
     * This method is invoked when {@link UploadEvent#reject()} or
     * {@link UploadEvent#reject(String)} is called, allowing the handler to
     * perform cleanup or logging for rejected uploads.
     * <p>
     * The default implementation does nothing. Override this method to handle
     * rejections.
     *
     * @param metadata
     *            the upload metadata containing relevant information about the
     *            rejected upload
     * @param reason
     *            the reason for rejection
     * @throws IOException
     *             if an I/O error occurs in the callback
     */
    default void rejected(UploadMetadata metadata, String reason)
            throws IOException {
        // Default implementation does nothing
    }
}
