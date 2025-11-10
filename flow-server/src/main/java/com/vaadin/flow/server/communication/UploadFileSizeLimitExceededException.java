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
package com.vaadin.flow.server.communication;

import java.io.IOException;

/**
 * Exception thrown when a single file in a multipart upload exceeds the
 * configured file size limit.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
class UploadFileSizeLimitExceededException extends IOException {

    private final String fileName;
    private final long actualSize;
    private final long permittedSize;

    /**
     * Constructs a new exception with the specified file name, actual and
     * permitted sizes.
     *
     * @param fileName
     *            the name of the file that exceeded the limit
     * @param actualSize
     *            the actual size of the file
     * @param permittedSize
     *            the maximum permitted file size
     */
    public UploadFileSizeLimitExceededException(String fileName,
            long actualSize, long permittedSize) {
        super(String.format(
                "File '%s' size (%d bytes) exceeds the permitted maximum (%d bytes)",
                fileName, actualSize, permittedSize));
        this.fileName = fileName;
        this.actualSize = actualSize;
        this.permittedSize = permittedSize;
    }

    /**
     * Gets the name of the file that exceeded the limit.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the actual size of the file.
     *
     * @return the actual size in bytes
     */
    public long getActualSize() {
        return actualSize;
    }

    /**
     * Gets the maximum permitted file size.
     *
     * @return the permitted size in bytes
     */
    public long getPermittedSize() {
        return permittedSize;
    }
}
