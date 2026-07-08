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
 * Validates an upload synchronously while it is being received, allowing it to
 * be refused before it is stored.
 * <p>
 * A validator has three lifecycle phases, each invoked on the request thread by
 * the pre-made upload handlers ({@link InMemoryUploadHandler},
 * {@link FileUploadHandler}, {@link TemporaryFileUploadHandler}):
 * <ul>
 * <li>{@link #validateMetadata(UploadEvent)} once, before any data is read, for
 * checks based only on metadata such as file name, content type or declared
 * size;</li>
 * <li>{@link #validateHeader(UploadEvent, ByteBuffer)} once, over the first
 * {@link #headerSize()} bytes of the upload, for checks on the leading content
 * such as file-signature ("magic byte") or MIME sniffing;</li>
 * <li>{@link #validateComplete(UploadEvent, UploadContent)} once, after the
 * whole upload has been received, for whole-content checks such as antivirus
 * scanning.</li>
 * </ul>
 * Calling {@link UploadEvent#reject(String)} from any phase aborts the upload:
 * no further data is read, the success callback is not invoked and any
 * partially stored data is cleaned up. Prefer {@code reject(...)} over
 * throwing: in a multipart upload a rejection lets the remaining files be
 * processed (HTTP 207), whereas a thrown exception aborts the whole request.
 * <p>
 * Validators run synchronously and <em>not</em> wrapped in
 * {@link com.vaadin.flow.component.UI#access(com.vaadin.flow.server.Command)};
 * they must only inspect the upload and decide whether to reject it, not
 * perform UI updates. Use a {@link TransferProgressListener} for UI updates.
 * <p>
 * For single-phase checks, prefer the fluent handler methods
 * ({@link AbstractUploadHandler#validateMetadata(UploadMetadataCallback)} and
 * friends) with a lambda. Implement this interface directly to combine multiple
 * phases in one validator.
 *
 * @see AbstractUploadHandler#withValidator(UploadValidator)
 * @see UploadEvent#reject(String)
 */
public interface UploadValidator extends Serializable {

    /**
     * Validates the upload metadata before any data is read.
     * <p>
     * Invoked once, before the first byte is read, so it can refuse an upload
     * (for example by declared size or file name) without reading its body.
     *
     * @param event
     *            the current upload
     * @throws IOException
     *             if validation fails; treated as a transfer error
     */
    default void validateMetadata(UploadEvent event) throws IOException {
    }

    /**
     * Validates the header (leading bytes) of the upload, before the rest is
     * read.
     * <p>
     * Invoked once, only when {@link #headerSize()} is greater than zero, with
     * a read-only view of the first {@code headerSize()} bytes of the upload
     * (or fewer, including an empty buffer, if the upload is smaller).
     * Rejecting here aborts the upload after only those leading bytes have been
     * read. The buffer is only valid for the duration of the call and must not
     * be retained.
     * <p>
     * <strong>Overriding this method without also overriding
     * {@link #headerSize()} to return a positive value has no effect: it is
     * never invoked.</strong>
     *
     * @param event
     *            the current upload
     * @param header
     *            a read-only view of the leading bytes of the upload
     * @throws IOException
     *             if validation fails; treated as a transfer error
     */
    default void validateHeader(UploadEvent event, ByteBuffer header)
            throws IOException {
    }

    /**
     * Validates the fully received upload, before it is delivered to the
     * success callback.
     * <p>
     * Invoked once, after the whole upload has been received. Because the
     * entire body has already been read by this point, this phase
     * <em>cannot</em> abort reading early, so it is not a size-limiting
     * mechanism. Note that {@link #validateMetadata} can only inspect the
     * client-declared size ({@link UploadEvent#getFileSize()}), which is
     * untrusted for XHR uploads, and {@link UploadHandler#getFileSizeMax()}
     * bounds multipart uploads only — XHR upload size is not enforced by the
     * framework.
     * <p>
     * The transfer's {@link TransferProgressListener#onComplete} has already
     * fired by the time this runs (it signals that all bytes were received, not
     * that the upload was accepted); rejecting or failing here is reported to
     * progress listeners as {@link TransferProgressListener#onError}.
     *
     * @param event
     *            the current upload
     * @param content
     *            handle to the received content, only valid for the duration of
     *            the call
     * @throws IOException
     *             if validation fails; treated as a transfer error
     */
    default void validateComplete(UploadEvent event, UploadContent content)
            throws IOException {
    }

    /**
     * The number of leading bytes to make available to
     * {@link #validateHeader(UploadEvent, ByteBuffer)}. Defaults to {@code 0},
     * meaning the header phase is skipped for this validator; override to a
     * positive value to receive the header.
     * <p>
     * Must return a stable value: it is queried more than once per upload. The
     * framework buffers this many bytes in memory per upload, so keep it
     * modest.
     *
     * @return the header size in bytes, {@code 0} to skip the header phase
     */
    default int headerSize() {
        return 0;
    }
}
