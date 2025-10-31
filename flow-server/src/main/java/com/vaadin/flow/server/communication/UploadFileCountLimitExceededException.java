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
 * Exception thrown when the number of files in a multipart upload exceeds the
 * configured limit.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
public class UploadFileCountLimitExceededException extends IOException {

    private final long actualCount;
    private final long permittedCount;

    /**
     * Constructs a new exception with the specified actual and permitted file
     * counts.
     *
     * @param actualCount
     *            the actual number of files
     * @param permittedCount
     *            the maximum permitted number of files
     */
    public UploadFileCountLimitExceededException(long actualCount,
            long permittedCount) {
        super(String.format(
                "Upload file count (%d) exceeds the permitted maximum (%d)",
                actualCount, permittedCount));
        this.actualCount = actualCount;
        this.permittedCount = permittedCount;
    }

    /**
     * Gets the actual number of files in the upload.
     *
     * @return the actual file count
     */
    public long getActualCount() {
        return actualCount;
    }

    /**
     * Gets the maximum permitted number of files.
     *
     * @return the permitted file count
     */
    public long getPermittedCount() {
        return permittedCount;
    }
}
