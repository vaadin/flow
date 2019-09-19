/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * An error thrown by the framework and handled by an {@link ErrorHandler}.
 * Typically handled by {@link VaadinSession#getErrorHandler()}.
 *
 * @since 1.0
 */
public class ErrorEvent implements Serializable {

    private final Throwable throwable;

    /**
     * Creates an error event which wraps the given throwable.
     *
     * @param throwable
     *            the throwable to wrap
     */
    public ErrorEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * Gets the contained throwable, the cause of the error.
     *
     * @return the throwable that caused the error
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Finds the error handler for the given session.
     *
     * @param session
     *            the active session
     * @return An ErrorHandler for the session or null if none was found
     */
    public static ErrorHandler findErrorHandler(VaadinSession session) {
        if (session == null) {
            return null;
        }
        return session.getErrorHandler();
    }

}
