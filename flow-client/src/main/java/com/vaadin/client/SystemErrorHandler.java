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
package com.vaadin.client;

import java.util.Set;

import com.google.web.bindery.event.shared.UmbrellaException;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/**
 * Handles system errors in the application. Pure {@code @JsType(isNative=true)}
 * binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/SystemErrorHandler.ts}.
 *
 * <p>
 * Construction takes a {@link SystemErrorHandlerCallbacks} adapter so the TS
 * class does not need to reach back through the Java {@code Registry} facade.
 * The Java side keeps the {@code Throwable} overload of {@link #handleError} so
 * {@code GWT.setUncaughtExceptionHandler} can use it as a method reference; the
 * body unwraps GWT's {@link UmbrellaException} and forwards a string to the TS
 * class.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "SystemErrorHandler")
public class SystemErrorHandler {

    public SystemErrorHandler(SystemErrorHandlerCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    /** Shows the session expiration notification. */
    public native void handleSessionExpiredError(String details);

    /** Shows an error notification for an unrecoverable error. */
    public native void handleUnrecoverableError(String caption, String message,
            String details, String url, String querySelector);

    /** Logs an error message. */
    public native void handleError(String errorMessage);

    /**
     * Convenience overload used by call sites that pass an {@link ErrorMessage}
     * value object.
     */
    @JsOverlay
    public final void handleUnrecoverableError(String details,
            com.vaadin.client.bootstrap.ErrorMessage message) {
        handleUnrecoverableError(message.getCaption(), message.getMessage(),
                details, message.getUrl(), null);
    }

    /**
     * Convenience overload that omits {@code querySelector}.
     */
    @JsOverlay
    public final void handleUnrecoverableError(String caption, String message,
            String details, String url) {
        handleUnrecoverableError(caption, message, details, url, null);
    }

    /**
     * Logs a throwable. Unwraps {@link UmbrellaException} so single-cause GWT
     * exceptions report the underlying message, and forwards a string to the TS
     * implementation so it doesn't need to know about {@link Throwable}. Kept
     * so {@code GWT.setUncaughtExceptionHandler} can use
     * {@code systemErrorHandler::handleError} as a method reference.
     */
    @JsOverlay
    public final void handleError(Throwable throwable) {
        Throwable unwrapped = unwrapUmbrellaException(throwable);
        if (unwrapped instanceof AssertionError) {
            handleError("Assertion error: " + unwrapped.getMessage());
        } else {
            handleError(unwrapped.getMessage());
        }
    }

    @JsOverlay
    private static Throwable unwrapUmbrellaException(Throwable e) {
        if (e instanceof UmbrellaException) {
            Set<Throwable> causes = ((UmbrellaException) e).getCauses();
            if (causes.size() == 1) {
                return unwrapUmbrellaException(causes.iterator().next());
            }
        }
        return e;
    }
}
