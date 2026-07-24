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

/**
 * Callback for validating a fully received upload before it is delivered, used
 * with {@link AbstractUploadHandler#validateComplete(UploadCompleteCallback)}.
 * <p>
 * Call {@link UploadEvent#reject(String)} to refuse the upload. See
 * {@link UploadValidator#validateComplete(UploadEvent, UploadContent)} for the
 * phase semantics; this runs only after the whole upload has been received, so
 * it cannot abort reading early.
 */
@FunctionalInterface
public interface UploadCompleteCallback extends Serializable {

    /**
     * Validates the fully received upload, rejecting it via
     * {@link UploadEvent#reject(String)} if it must not be delivered.
     *
     * @param event
     *            the current upload
     * @param content
     *            handle to the received content; only valid for the duration of
     *            the call
     * @throws IOException
     *             if validation fails; treated as a transfer error
     */
    void validate(UploadEvent event, UploadContent content) throws IOException;
}
