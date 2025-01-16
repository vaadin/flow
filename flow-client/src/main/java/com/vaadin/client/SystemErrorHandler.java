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
package com.vaadin.client;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import com.google.web.bindery.event.shared.UmbrellaException;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.xhr.client.XMLHttpRequest;

import com.vaadin.client.bootstrap.ErrorMessage;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.gwt.elemental.js.util.Xhr;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.util.SharedUtil;

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
                details, message.getUrl(), null);
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
        handleUnrecoverableError(caption, message, details, url, null);
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
     * @param querySelector
     *            query selector to find the element under which the error will
     *            be added . If element is not found or the selector is
     *            {@code null}, body will be used
     */
    public void handleUnrecoverableError(String caption, String message,
            String details, String url, String querySelector) {
        if (caption == null && message == null && details == null) {
            if (!isWebComponentMode()) {
                WidgetUtil.redirect(url);
            } else {
                resynchronizeSession();
            }
            return;
        }

        Element systemErrorContainer = handleError(caption, message, details,
                querySelector);
        if (!isWebComponentMode()) {
            systemErrorContainer.addEventListener("click",
                    e -> WidgetUtil.redirect(url), false);
            Browser.getDocument().addEventListener(Event.KEYDOWN, e -> {
                int keyCode = ((KeyboardEvent) e).getKeyCode();
                if (keyCode == KeyCode.ESC) {
                    e.preventDefault();
                    WidgetUtil.redirect(url);
                }
            }, false);
        }
    }

    private boolean resyncInProgress = false;

    /**
     * Send GET async request to acquire new JSESSIONID, browser will set cookie
     * automatically based on Set-Cookie response header.
     */
    private void resynchronizeSession() {
        if (resyncInProgress) {
            Console.debug(
                    "Web components resynchronization already in progress");
            return;
        }
        resyncInProgress = true;
        String serviceUrl = registry.getApplicationConfiguration()
                .getServiceUrl() + "web-component/web-component-bootstrap.js";

        // Stop heart beat to prevent requests during resynchronization
        registry.getHeartbeat().setInterval(-1);
        if (registry.getPushConfiguration().isPushEnabled()) {
            registry.getMessageSender().setPushEnabled(false, false);
        }

        String sessionResyncUri = SharedUtil.addGetParameter(serviceUrl,
                ApplicationConstants.REQUEST_TYPE_PARAMETER,
                ApplicationConstants.REQUEST_TYPE_WEBCOMPONENT_RESYNC);

        Xhr.getWithCredentials(sessionResyncUri, new Xhr.Callback() {
            @Override
            public void onFail(XMLHttpRequest xhr, Exception exception) {
                registry.getHeartbeat().setInterval(registry
                        .getApplicationConfiguration().getHeartbeatInterval());
                handleError(exception);
            }

            @Override
            public void onSuccess(XMLHttpRequest xhr) {
                Console.log(
                        "Received xhr HTTP session resynchronization message: "
                                + xhr.getResponseText());

                // Make sure heartbeat has not been restarted. This is
                // especially important if the uiId gets reset after session
                // expiration, to prevent multiple heartbeats requests for
                // different ui
                registry.getHeartbeat().setInterval(-1);

                int uiId = registry.getApplicationConfiguration().getUIId();
                ValueMap json = MessageHandler
                        .parseWrappedJson(xhr.getResponseText());
                int newUiId = json.getInt(ApplicationConstants.UI_ID);
                if (newUiId != uiId) {
                    Console.debug("UI ID switched from " + uiId + " to "
                            + newUiId + " after resynchronization");
                    registry.getApplicationConfiguration().setUIId(newUiId);
                }
                registry.reset();

                registry.getUILifecycle().setState(UILifecycle.UIState.RUNNING);
                registry.getMessageHandler().handleMessage(json);

                boolean pushEnabled = registry.getPushConfiguration()
                        .isPushEnabled();
                if (pushEnabled) {
                    // PUSH connection might have been closed in response to
                    // sever session expiration. If PUSH is required, reconnect
                    // before recreating web components to make sure the
                    // connected events can be propagated to the server.
                    // PUSH reconnection is deferred to allow current request
                    // to complete and process the Set-Cookie header.
                    Scheduler.get().scheduleDeferred(() -> {
                        Console.debug("Re-establish PUSH connection");
                        registry.getMessageSender().setPushEnabled(true);
                        Scheduler.get().scheduleDeferred(
                                () -> recreateWebComponents());
                    });
                } else {
                    Scheduler.get()
                            .scheduleDeferred(() -> recreateWebComponents());
                }
            }
        });
    }

    private void recreateWebComponents() {
        Arrays.stream(registry.getApplicationConfiguration()
                .getExportedWebComponents())
                .forEach(SystemErrorHandler.this::recreateNodes);
        resyncInProgress = false;
    }

    private native void recreateNodes(String elementName)
    /*-{
        var elements = document.getElementsByTagName(elementName);
        for (var i = 0 ; i < elements.length ; ++i) {
            var elem = elements[i];
            elem.$server.disconnected = function(){} // mock disconnected callback not to throw TypeError
            elem.parentNode.replaceChild(elem.cloneNode(false), elem);
        }
    }-*/;

    /**
     * Shows the given error message if not running in production mode and logs
     * it to the console if running in production mode.
     *
     * @param errorMessage
     *            the error message to show
     */
    public void handleError(String errorMessage) {
        Console.error(errorMessage);
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

    private Element handleError(String caption, String message, String details,
            String querySelector) {
        Document document = Browser.getDocument();
        Element systemErrorContainer = document.createDivElement();
        systemErrorContainer.setClassName("v-system-error");

        if (caption != null) {
            Element captionDiv = document.createDivElement();
            captionDiv.setClassName("caption");
            captionDiv.setTextContent(caption);
            systemErrorContainer.appendChild(captionDiv);
            Console.error(caption);
        }
        if (message != null) {
            Element messageDiv = document.createDivElement();
            messageDiv.setClassName("message");
            messageDiv.setTextContent(message);
            systemErrorContainer.appendChild(messageDiv);
            Console.error(message);
        }
        if (details != null) {
            Element detailsDiv = document.createDivElement();
            detailsDiv.setClassName("details");
            detailsDiv.setTextContent(details);
            systemErrorContainer.appendChild(detailsDiv);
            Console.error(details);
        }
        if (querySelector != null) {
            Element baseElement = document.querySelector(querySelector);
            // if querySelector does not match an element on the page, the
            // error will not be displayed
            if (baseElement != null) {
                // if the baseElement has a shadow root, add the warning to
                // the shadow - otherwise add it to the baseElement
                findShadowRoot(baseElement).orElse(baseElement)
                        .appendChild(systemErrorContainer);
            }
        } else {
            document.getBody().appendChild(systemErrorContainer);
        }

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

    private Optional<Element> findShadowRoot(Element host) {
        return Optional.ofNullable(getShadowRootElement(host));
    }

    private boolean isWebComponentMode() {
        return registry.getApplicationConfiguration().isWebComponentMode();
    }

    private native Element getShadowRootElement(Element host)
    /*-{
        return host.shadowRoot;
    }-*/;

}
