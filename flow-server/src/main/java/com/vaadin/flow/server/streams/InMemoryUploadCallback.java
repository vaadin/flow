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
     * @param data
     *            the byte array containing the uploaded data
     * @throws IOException
     *             if an I/O error occurs in the callback
     */
    void complete(UploadMetadata metadata, byte[] data) throws IOException;
}
