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
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A handle to a fully received upload, passed to
 * {@link UploadValidator#validateComplete(UploadEvent, UploadContent)} so that
 * whole-content checks (for example antivirus scanning) can inspect the data
 * before it is delivered to the success callback.
 * <p>
 * The content is only valid for the duration of the {@code validateComplete}
 * call and must not be retained: for a file-backed upload the underlying file
 * may be deleted immediately after the call (for example when the upload is
 * rejected), and any {@link #getInputStream() stream} handed out is closed by
 * the framework once the call returns.
 *
 * @see UploadValidator#validateComplete(UploadEvent, UploadContent)
 */
public interface UploadContent extends Serializable {

    /**
     * Opens a new stream over the received content.
     * <p>
     * Each call returns an independent stream positioned at the start. The
     * framework closes any stream returned here once the enclosing
     * {@code validateComplete} call returns, so a validator must read within
     * that call and must not retain the stream. When {@link #asPath()} is
     * present, prefer it for libraries that scan by file path.
     *
     * @return a fresh input stream over the content
     * @throws IOException
     *             if the stream cannot be opened
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns the size of the received content in bytes.
     *
     * @return the content size in bytes
     */
    long size();

    /**
     * Returns the path of the file backing this content, if the upload was
     * stored to a file.
     * <p>
     * Present for file-based upload handlers
     * ({@link FileUploadHandler}/{@link TemporaryFileUploadHandler}) and empty
     * for in-memory uploads. The path refers to the file produced by the
     * handler's {@link FileFactory}; it may be deleted right after
     * {@code validateComplete} returns if the upload is rejected.
     * <p>
     * Do not hand this path to an asynchronous or background process: read or
     * scan it synchronously within the {@code validateComplete} call, since the
     * file may be gone once the call returns.
     *
     * @return the backing file path, or empty if the content is not file-backed
     */
    Optional<Path> asPath();
}
