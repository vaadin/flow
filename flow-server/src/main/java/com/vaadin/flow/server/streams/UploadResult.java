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

import com.vaadin.flow.server.VaadinResponse;

/**
 * Represents the result of an upload operation.
 * <p>
 * This record encapsulates the outcome of processing an upload request,
 * including whether it was successful, the response object, and any exception
 * that may have occurred.
 *
 * @param success
 *            {@code true} if the upload was successful, {@code false} otherwise
 * @param response
 *            the response object for the upload request
 * @param exception
 *            the exception that caused the failure, or {@code null} if
 *            successful or no exception available
 */
public record UploadResult(boolean success, VaadinResponse response,
        Exception exception) implements Serializable {

    /**
     * Creates an upload result without an exception.
     *
     * @param success
     *            {@code true} if the upload was successful, {@code false}
     *            otherwise
     * @param response
     *            the response object for the upload request
     */
    public UploadResult(boolean success, VaadinResponse response) {
        this(success, response, null);
    }
}
