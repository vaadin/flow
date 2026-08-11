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

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Callback for validating the leading bytes of an upload, used with
 * {@link AbstractUploadHandler#validateHeader(int, UploadHeaderCallback)}.
 * <p>
 * Call {@link UploadEvent#reject(String)} to refuse the upload. See
 * {@link UploadValidator#validateHeader(UploadEvent, ByteBuffer)} for the phase
 * semantics.
 */
@FunctionalInterface
public interface UploadHeaderCallback extends Serializable {

    /**
     * Validates the header (leading bytes) of an upload, rejecting it via
     * {@link UploadEvent#reject(String)} if it must not be stored.
     *
     * @param event
     *            the current upload
     * @param header
     *            a read-only view of the leading bytes of the upload, up to the
     *            requested size (fewer if the upload is smaller); only valid
     *            for the duration of the call
     * @throws IOException
     *             if validation fails; treated as a transfer error
     */
    void validate(UploadEvent event, ByteBuffer header) throws IOException;
}
