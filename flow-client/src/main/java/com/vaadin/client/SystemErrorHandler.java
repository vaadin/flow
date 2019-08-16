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
package com.vaadin.client;

import java.util.Set;

import com.google.web.bindery.event.shared.UmbrellaException;
import com.vaadin.client.bootstrap.ErrorMessage;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;

/**
 * Handles system errors in the application.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SystemErrorHandler {

    private Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public SystemErrorHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Shows the session expiration notification.
     *
     * @param details
     *            message details or null if there are no details
     */
    public void handleSessionExpiredError(String details) {
        handleUnrecoverableError(details, registry.getApplicationConfiguration()
                .getSessionExpiredError());
    }

    /**
     * Shows an error notification for an error which is unrecoverable.
     *
     * @param details
     *            message details or null if there are no details
     * @param message
     *            an ErrorMessage describing the error
     */
    protected void handleUnrecoverableError(String details,
            ErrorMessage message) {
        handleUnrecoverableError(message.getCaption(), message.getMessage(),
                details, message.getUrl());
    }

    /**
     * Shows an error notification for an error which is unrecoverable, using
     * the given parameters.
     *
     * @param caption
     *            the caption of the message
     * @param message
     *            the message body
     * @param details
     *            message details or {@code null} if there are no details
     * @param url
     *            a URL to redirect to when the user clicks the message or
     *            {@code null} to refresh on click
     */
    public void handleUnrecoverableError(String caption, String message,
            String details, String url) {
        if (caption == null && message == null && details == null) {
            WidgetUtil.redirect(url);
            return;
        }

        Element systemErrorContainer = handleError(caption, message, details);
        systemErrorContainer.addEventListener("click",
                e -> WidgetUtil.redirect(url), false);

        Browser.getDocument().addEventListener(Event.KEYDOWN, e -> {
            int keyCode = ((KeyboardEvent) e).getKeyCode();
            if (keyCode == KeyCode.ESC) {
                WidgetUtil.redirect(url);
            }
        }, false);
    }

    /**
     * Shows the given error message if not running in production mode and logs
     * it to the console if running in production mode.
     *
     * @param errorMessage
     *            the error message to show
     */
    public void handleError(String errorMessage) {
        if (registry.getApplicationConfiguration().isProductionMode()) {
            Console.error(errorMessage);
            return;
        }

        Element errorContainer = handleError(null, errorMessage, null);
        errorContainer.addEventListener("click", e -> {
            // Allow user to dismiss the error by clicking it.
            errorContainer.getParentElement().removeChild(errorContainer);
        });
    }

    /**
     * Shows an error message if not running in production mode and logs it to
     * the console if running in production mode.
     *
     * @param throwable
     *            the throwable which occurred
     */
    public void handleError(Throwable throwable) {
        Throwable unwrappedThrowable = unwrapUmbrellaException(throwable);
        if (unwrappedThrowable instanceof AssertionError) {
            handleError("Assertion error: " + unwrappedThrowable.getMessage());
        } else {
            handleError(unwrappedThrowable.getMessage());
        }
    }

    private Element handleError(String caption, String message,
            String details) {
        Document document = Browser.getDocument();
        Element systemErrorContainer = document.createDivElement();
        systemErrorContainer.setClassName("v-system-error");

        if (caption != null) {
            Element captionDiv = document.createDivElement();
            captionDiv.setClassName("caption");
            captionDiv.setInnerHTML(caption);
            systemErrorContainer.appendChild(captionDiv);
            Console.error(caption);
        }
        if (message != null) {
            Element messageDiv = document.createDivElement();
            messageDiv.setClassName("message");
            messageDiv.setInnerHTML(message);
            systemErrorContainer.appendChild(messageDiv);
            Console.error(message);
        }
        if (details != null) {
            Element detailsDiv = document.createDivElement();
            detailsDiv.setClassName("details");
            detailsDiv.setInnerHTML(details);
            systemErrorContainer.appendChild(detailsDiv);
            Console.error(details);
        }

        document.getBody().appendChild(systemErrorContainer);
        return systemErrorContainer;
    }

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
