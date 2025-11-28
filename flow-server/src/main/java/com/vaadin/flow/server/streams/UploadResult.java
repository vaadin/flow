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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.server.VaadinResponse;

/**
 * Represents the result of an upload operation.
 * <p>
 * This record encapsulates the outcome of processing an upload request,
 * including whether it was successful, the response object, any exception that
 * may have occurred, and lists of accepted and rejected files.
 *
 * @param success
 *            {@code true} if the upload was successful, {@code false} otherwise
 * @param response
 *            the response object for the upload request
 * @param exception
 *            the exception that caused the failure, or {@code null} if
 *            successful or no exception available
 * @param acceptedFiles
 *            list of file names that were accepted
 * @param rejectedFiles
 *            list of rejected files with their rejection reasons
 */
public record UploadResult(boolean success, VaadinResponse response,
        Exception exception, List<String> acceptedFiles,
        List<RejectedFile> rejectedFiles) implements Serializable {

    /**
     * Represents a rejected file with its rejection reason.
     *
     * @param fileName
     *            the name of the rejected file
     * @param reason
     *            the reason for rejection
     */
    public record RejectedFile(String fileName,
            String reason) implements Serializable {
    }

    /**
     * Creates an upload result without an exception or file tracking.
     *
     * @param success
     *            {@code true} if the upload was successful, {@code false}
     *            otherwise
     * @param response
     *            the response object for the upload request
     */
    public UploadResult(boolean success, VaadinResponse response) {
        this(success, response, null, Collections.emptyList(),
                Collections.emptyList());
    }

    /**
     * Creates an upload result with an exception but no file tracking.
     *
     * @param success
     *            {@code true} if the upload was successful, {@code false}
     *            otherwise
     * @param response
     *            the response object for the upload request
     * @param exception
     *            the exception that caused the failure
     */
    public UploadResult(boolean success, VaadinResponse response,
            Exception exception) {
        this(success, response, exception, Collections.emptyList(),
                Collections.emptyList());
    }

    /**
     * Checks if all files were accepted.
     *
     * @return {@code true} if there are accepted files and no rejected files
     */
    public boolean allAccepted() {
        return !acceptedFiles.isEmpty() && rejectedFiles.isEmpty();
    }

    /**
     * Checks if all files were rejected.
     *
     * @return {@code true} if there are rejected files and no accepted files
     */
    public boolean allRejected() {
        return !rejectedFiles.isEmpty() && acceptedFiles.isEmpty();
    }

    /**
     * Checks if there is a mix of accepted and rejected files.
     *
     * @return {@code true} if there are both accepted and rejected files
     */
    public boolean hasMixed() {
        return !acceptedFiles.isEmpty() && !rejectedFiles.isEmpty();
    }

    /**
     * Checks if any files were processed.
     *
     * @return {@code true} if there are any accepted or rejected files
     */
    public boolean hasFiles() {
        return !acceptedFiles.isEmpty() || !rejectedFiles.isEmpty();
    }
}
