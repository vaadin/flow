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

/**
 * Signals that an upload was rejected by an {@link UploadValidator}.
 * <p>
 * This is passed as the reason to
 * {@link TransferProgressListener#onError(TransferContext, IOException)} when a
 * validator calls {@link UploadEvent#reject(String)}, so that listeners can
 * tell a deliberate rejection apart from a genuine I/O failure. Its message is
 * the rejection message supplied to {@link UploadEvent#reject(String)}.
 *
 * @see UploadValidator
 * @see UploadEvent#reject(String)
 */
public class UploadRejectedException extends IOException {

    /**
     * Creates a new exception with the given rejection message.
     *
     * @param message
     *            the rejection message
     */
    public UploadRejectedException(String message) {
        super(message);
    }
}
