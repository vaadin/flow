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
package com.vaadin.flow.server.frontend.installer;

/**
 * Exception indicating a failure during installation of node and npm.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class InstallationException extends Exception {
    /**
     * Exception with message.
     *
     * @param message
     *            exception message
     */
    public InstallationException(String message) {
        super(message);
    }

    /**
     * Exceptioon with message and cause.
     *
     * @param message
     *            exception message
     * @param cause
     *            cause for exception
     */
    public InstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
