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

/**
 * Exception thrown when multipart upload is attempted but the servlet is not
 * configured to handle multipart requests.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
public class MultipartConfigurationException extends RuntimeException {

    /**
     * The standard error message explaining multipart configuration
     * requirements.
     */
    public static final String MULTIPART_CONFIG_MISSING_MESSAGE = "Multipart upload requires the servlet to be configured with @MultipartConfig annotation. "
            + "Note that Vaadin components use XHR (XMLHttpRequest) uploads by default since version 25.0, "
            + "which do not require multipart configuration. "
            + "If you need to support traditional multipart form uploads, "
            + "add @MultipartConfig to your servlet or configure multipart handling in web.xml.";

    /**
     * Constructs a new exception with a detailed message explaining the
     * configuration requirement.
     */
    public MultipartConfigurationException() {
        super(MULTIPART_CONFIG_MISSING_MESSAGE);
    }

    /**
     * Constructs a new exception with the standard message and a cause.
     *
     * @param cause
     *            the cause of this exception
     */
    public MultipartConfigurationException(Throwable cause) {
        super(MULTIPART_CONFIG_MISSING_MESSAGE, cause);
    }
}
