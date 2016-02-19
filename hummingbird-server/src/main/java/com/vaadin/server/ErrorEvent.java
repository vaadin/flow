/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server;

import java.io.Serializable;

/**
 * An error thrown by the framework and handled by an {@link ErrorHandler}.
 * Typically handled by {@link VaadinSession#getErrorHandler()}.
 */
public class ErrorEvent implements Serializable {

    private Throwable throwable;

    public ErrorEvent(Throwable t) {
        setThrowable(t);
    }

    /**
     * Gets the contained throwable, the cause of the error.
     *
     * @return
     */
    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * Method for finding the error handler for the given session.
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
