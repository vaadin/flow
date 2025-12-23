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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.SessionExpiredHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Handles requests to open an SSE (Server-Sent Events) push connection between
 * the client and the server. SSE is a unidirectional server-to-client channel;
 * client-to-server messages continue to use XHR.
 * <p>
 * This handler does not use the Atmosphere framework.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.7
 */
public class SseRequestHandler
        implements RequestHandler, SessionExpiredHandler {

    private final VaadinServletService service;

    /**
     * Creates an instance connected to the given service.
     *
     * @param service
     *            the service this handler belongs to
     */
    public SseRequestHandler(VaadinServletService service) {
        this.service = service;
        service.addServiceDestroyListener(event -> destroy());
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        if (!HandlerHelper.isRequestType(request, RequestType.SSE)) {
            return false;
        }

        if (!(request instanceof VaadinServletRequest)
                || !(response instanceof VaadinServletResponse)) {
            throw new IllegalArgumentException(
                    "Only VaadinServletRequest/Response are supported for SSE");
        }

        HttpServletRequest servletRequest = (HttpServletRequest) ((VaadinServletRequest) request)
                .getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) ((VaadinServletResponse) response)
                .getResponse();

        // Check if async is supported
        if (!servletRequest.isAsyncSupported()) {
            getLogger().error("Async not supported. SSE push unavailable.");
            servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Async not supported. SSE push unavailable.");
            return true;
        }

        // Validate session
        if (session == null) {
            getLogger().debug("Session expired, sending session expired event");
            sendSessionExpired(servletResponse);
            return true;
        }

        // Find UI
        UI ui;
        session.lock();
        try {
            ui = service.findUI(request);
            if (ui == null) {
                getLogger().warn("UI not found for SSE request");
                sendError(servletResponse, "UI not found");
                return true;
            }

            // Validate push ID (CSRF protection)
            String requestPushId = request
                    .getParameter(ApplicationConstants.PUSH_ID_PARAMETER);
            if (!isPushIdValid(session, requestPushId)) {
                getLogger().warn("Invalid push ID received from {}",
                        servletRequest.getRemoteHost());
                sendRefresh(servletResponse);
                return true;
            }

            // Get the SSE push connection
            PushConnection pushConnection = ui.getInternals()
                    .getPushConnection();
            if (!(pushConnection instanceof SsePushConnection)) {
                getLogger().warn(
                        "SSE request received but UI does not have SsePushConnection. "
                                + "Connection type: {}",
                        pushConnection != null
                                ? pushConnection.getClass().getName()
                                : "null");
                sendError(servletResponse, "SSE not configured for this UI");
                return true;
            }

            SsePushConnection sseConnection = (SsePushConnection) pushConnection;

            // Start async context
            AsyncContext asyncContext = servletRequest.startAsync();
            asyncContext.setTimeout(0); // No timeout for SSE

            // Set up async listener for connection lifecycle
            asyncContext.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    handleDisconnect(sseConnection);
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    getLogger().debug("SSE connection timed out");
                    handleDisconnect(sseConnection);
                }

                @Override
                public void onError(AsyncEvent event) {
                    getLogger().debug("SSE connection error",
                            event.getThrowable());
                    handleDisconnect(sseConnection);
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                    // Re-register listener if async restarted
                }
            });

            // Connect the SSE push connection
            sseConnection.connect(asyncContext);

            getLogger().debug("SSE connection established for UI {}",
                    ui.getUIId());

        } finally {
            session.unlock();
        }

        return true;
    }

    /**
     * Handles disconnection of an SSE connection.
     *
     * @param connection
     *            the connection that was disconnected
     */
    private void handleDisconnect(SsePushConnection connection) {
        getLogger().debug("SSE connection disconnected");
        connection.connectionLost();
    }

    /**
     * Checks whether a given push id matches the session's push id. The
     * comparison is done using a time-constant method since the push id is used
     * to protect against cross-site attacks.
     *
     * @param session
     *            the vaadin session for which the check should be done
     * @param requestPushId
     *            the push id provided in the request
     * @return {@code true} if the id is valid, {@code false} otherwise
     */
    private static boolean isPushIdValid(VaadinSession session,
            String requestPushId) {
        String sessionPushId = session.getPushId();
        if (requestPushId == null || !MessageDigest.isEqual(
                requestPushId.getBytes(StandardCharsets.UTF_8),
                sessionPushId.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        return true;
    }

    /**
     * Sends a session expired event to the client.
     *
     * @param response
     *            the response to write to
     * @throws IOException
     *             if writing fails
     */
    private void sendSessionExpired(HttpServletResponse response)
            throws IOException {
        response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
        response.getWriter()
                .write(VaadinService.createSessionExpiredJSON(true));
    }

    /**
     * Sends a refresh command to the client.
     *
     * @param response
     *            the response to write to
     * @throws IOException
     *             if writing fails
     */
    private void sendRefresh(HttpServletResponse response) throws IOException {
        response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
        response.getWriter()
                .write(VaadinService.createCriticalNotificationJSON(null, null,
                        null, null));
    }

    /**
     * Sends an error message to the client.
     *
     * @param response
     *            the response to write to
     * @param message
     *            the error message
     * @throws IOException
     *             if writing fails
     */
    private void sendError(HttpServletResponse response, String message)
            throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
    }

    @Override
    public boolean handleSessionExpired(VaadinRequest request,
            VaadinResponse response) throws IOException {
        // Handle session expired for SSE requests
        if (!HandlerHelper.isRequestType(request, RequestType.SSE)) {
            return false;
        }
        return handleRequest(null, request, response);
    }

    /**
     * Frees any resources currently in use.
     */
    public void destroy() {
        // No resources to free for SSE (no Atmosphere framework)
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SseRequestHandler.class.getName());
    }
}
