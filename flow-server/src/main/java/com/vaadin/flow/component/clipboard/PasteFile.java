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
package com.vaadin.flow.component.clipboard;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableConsumer;

/**
 * A single file delivered to a {@link Clipboard#onFilePaste} listener through
 * {@link PasteFileHandler#perFile(SerializableConsumer)
 * PasteFileHandler.perFile} or the {@code onFile} step of a
 * {@link PasteFileHandler#batch() batch}. Carries the file's bytes plus the
 * metadata needed to render it ({@link #fileName()}, {@link #contentType()},
 * {@link #size()}) and the correlation needed to group it with the rest of the
 * paste it came from ({@link #pasteId()}, {@link #newPaste()},
 * {@link #totalFiles()}).
 * <p>
 * The {@link #pasteId() paste id} is the monotonic sequence number emitted by
 * the client paste-upload helper (see {@link Clipboard#PASTE_ID_HEADER}). All
 * files originating from the same paste gesture share one id; subsequent pastes
 * carry strictly larger ids. {@link #newPaste()} is {@code true} on the first
 * file of each paste to reach the listener. Every file the browser uploads is
 * delivered and pastes complete independently, even when their uploads
 * interleave in transit, so application code wanting a "show the latest paste
 * only" UI tracks the highest paste id seen and filters in its own callback.
 * <p>
 * {@link #fileName()} is supplied by the browser and is <em>not</em> sanitized;
 * treat it as untrusted and never use it directly as a filesystem path without
 * sanitizing it first.
 *
 * @param pasteId
 *            the paste sequence number this file belongs to
 * @param newPaste
 *            whether this is the first file of a new paste reaching the
 *            listener; subsequent files of the same paste arrive with
 *            {@code false}
 * @param totalFiles
 *            the total number of files the originating paste contained, as
 *            reported by the browser (see
 *            {@link Clipboard#PASTE_FILE_COUNT_HEADER})
 * @param fileName
 *            the original file name as reported by the browser
 * @param contentType
 *            the MIME type as reported by the browser, or {@code null} when the
 *            browser did not provide one
 * @param size
 *            the size of the uploaded body in bytes
 * @param bytes
 *            the uploaded body
 */
public record PasteFile(long pasteId, boolean newPaste, int totalFiles,
        String fileName, @Nullable String contentType, long size,
        byte[] bytes) implements Serializable {
}
