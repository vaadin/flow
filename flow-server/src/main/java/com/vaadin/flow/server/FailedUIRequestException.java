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
package com.vaadin.flow.server;

import com.vaadin.flow.component.UI;

/**
 * Carries the exception that made a {@link SynchronizedRequestHandler} fail
 * together with the UI that was current when it failed.
 * <p>
 * The handler restores the current instances when it unlocks the session, so
 * the UI is no longer current when the exception reaches the error handling in
 * {@link VaadinService}. The error handling unwraps the exception and makes the
 * UI current again while it holds the session lock. Other code that calls
 * {@link SynchronizedRequestHandler#handleRequest(VaadinSession, VaadinRequest, VaadinResponse)}
 * directly can catch this exception and handle the original exception from
 * {@link #getFailure()}.
 *
 * @since 25.4
 */
public class FailedUIRequestException extends RuntimeException {

    private final transient UI ui;

    FailedUIRequestException(Exception failure, UI ui) {
        super(failure);
        this.ui = ui;
    }

    /**
     * Gets the exception that made the request handler fail.
     *
     * @return the exception, not <code>null</code>
     */
    public Exception getFailure() {
        return (Exception) getCause();
    }

    /**
     * Gets the UI that was current when the request handler failed.
     *
     * @return the UI, or <code>null</code> if this exception has been
     *         deserialized
     */
    public UI getUI() {
        return ui;
    }
}
