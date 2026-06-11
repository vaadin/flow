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

/**
 * Fired by the {@code onComplete} step of a {@link PasteFileHandler#batch()}
 * once all expected files of a paste have been delivered to the {@code onFile}
 * step.
 * <p>
 * "Expected" comes from the browser's {@link Clipboard#PASTE_FILE_COUNT_HEADER}
 * value; uploads that fail in transit (network errors, rejected uploads) never
 * arrive at the server and therefore do not count, so a paste with a lost
 * upload never fires {@code onComplete}. Starting a newer paste supersedes an
 * older one that has not finished: the superseded paste is abandoned (its
 * {@code onComplete} never fires) and any late upload of it is dropped, so a
 * stalled or malformed batch cannot linger.
 * <p>
 * {@code onComplete} reflects only the files this batch successfully received;
 * it is not a server-side error channel. To observe per-file outcomes &mdash;
 * including server-side processing failures &mdash; pass a custom
 * {@link com.vaadin.flow.server.streams.UploadHandler UploadHandler} (or a
 * {@link com.vaadin.flow.server.streams.TransferProgressListener
 * TransferProgressListener}) to
 * {@link Clipboard#onFilePaste(com.vaadin.flow.component.Component, com.vaadin.flow.server.streams.UploadHandler)
 * onFilePaste} and react to each upload there, e.g.:
 *
 * <pre>{@code
 * UploadHandler.inMemory((metadata, bytes) -> {
 *     // handle the bytes
 * }).whenComplete(success -> {
 *     if (!success) {
 *         // handle the server-side error
 *     }
 * });
 * }</pre>
 *
 * @param pasteId
 *            the paste sequence number that just finished delivering
 * @param receivedFiles
 *            the number of files actually delivered for this paste; equals the
 *            {@link PasteStart#totalFiles()} value the paste started with
 */
public record PasteComplete(long pasteId,
        int receivedFiles) implements Serializable {
}
