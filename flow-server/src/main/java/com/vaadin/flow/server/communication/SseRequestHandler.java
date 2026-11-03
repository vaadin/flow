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
package com.vaadin.flow.server.communication;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.InitParameters;
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
 */
public class SseRequestHandler
        implements RequestHandler, SessionExpiredHandler {

    private static final int DEFAULT_KEEP_ALIVE_INTERVAL = 30;

    private final VaadinServletService service;

    private transient ScheduledExecutorService keepAliveExecutor;

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
            servletResponse.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Async not supported. SSE push unavailable.");
            return true;
        }

        // Validate session
        if (session == null) {
            getLogger().debug("Session expired, sending session expired event");
            sendNotification(servletResponse,
                    VaadinService.createSessionExpiredJSON(true));
            return true;
        }

        // Find UI
        UI ui;
        session.lock();
        try {
            ui = service.findUI(request);
            if (ui == null) {
                getLogger().warn("UI not found for SSE request");
                sendNotification(servletResponse,
                        VaadinService.createUINotFoundJSON(true));
                return true;
            }

            // Validate push ID (CSRF protection)
            String requestPushId = request
                    .getParameter(ApplicationConstants.PUSH_ID_PARAMETER);
            if (!isPushIdValid(session, requestPushId)) {
                getLogger().warn("Invalid push ID received from {}",
                        servletRequest.getRemoteHost());
                sendNotification(servletResponse,
                        VaadinService.createCriticalNotificationJSON(null, null,
                                null, null));
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
                servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "SSE not configured for this UI");
                return true;
            }

            SsePushConnection sseConnection = (SsePushConnection) pushConnection;

            // Start async context
            AsyncContext asyncContext = servletRequest.startAsync();
            asyncContext.setTimeout(0); // No timeout for SSE

            SseStream stream = new SseStream(sseConnection, asyncContext);
            asyncContext.addListener(stream);
            sseConnection.connect(asyncContext);
            int keepAliveInterval = getKeepAliveInterval();
            if (keepAliveInterval > 0) {
                Executor executor = service.getExecutor();
                stream.keepAlive = getKeepAliveExecutor()
                        .scheduleWithFixedDelay(
                                () -> stream.sendKeepAlive(executor),
                                keepAliveInterval, keepAliveInterval,
                                TimeUnit.SECONDS);
            }

            getLogger().debug("SSE connection established for UI {}",
                    ui.getUIId());

        } finally {
            session.unlock();
        }

        return true;
    }

    // Read per stream rather than in the constructor, so that creating the
    // service does not depend on the configuration, e.g. a mocked one in tests
    private int getKeepAliveInterval() {
        return service.getDeploymentConfiguration()
                .getApplicationOrSystemProperty(
                        InitParameters.SERVLET_PARAMETER_PUSH_SSE_KEEP_ALIVE_INTERVAL,
                        DEFAULT_KEEP_ALIVE_INTERVAL, Integer::parseInt);
    }

    /**
     * Returns the thread that times the keep-alive comments of all streams. It
     * only hands the writing to the executor of the service, as a write may
     * wait for a slow client.
     */
    synchronized ScheduledExecutorService getKeepAliveExecutor() {
        if (keepAliveExecutor == null) {
            keepAliveExecutor = Executors
                    .newSingleThreadScheduledExecutor(runnable -> {
                        Thread thread = new Thread(runnable,
                                "vaadin-sse-push-keep-alive");
                        thread.setDaemon(true);
                        return thread;
                    });
        }
        return keepAliveExecutor;
    }

    /**
     * An open event stream of a push connection. Keeps the stream alive while
     * it is open, and marks the push connection lost when it ends.
     */
    private static class SseStream
            implements AsyncListener, Runnable, Serializable {

        private final SsePushConnection connection;
        private final transient AsyncContext asyncContext;
        private transient volatile ScheduledFuture<?> keepAlive;
        private final transient AtomicBoolean sending = new AtomicBoolean();

        private SseStream(SsePushConnection connection,
                AsyncContext asyncContext) {
            this.connection = connection;
            this.asyncContext = asyncContext;
        }

        private void sendKeepAlive(Executor executor) {
            // Skipped while the previous comment still waits for the client
            if (!sending.compareAndSet(false, true)) {
                return;
            }
            try {
                executor.execute(this);
            } catch (RuntimeException e) {
                sending.set(false);
                getLogger().debug("Could not send a keep-alive comment", e);
            }
        }

        @Override
        public void run() {
            try {
                if (!connection.sendKeepAlive(asyncContext)) {
                    stopKeepAlive();
                }
            } finally {
                sending.set(false);
            }
        }

        private void ended() {
            stopKeepAlive();
            connection.connectionLost(asyncContext);
        }

        private void stopKeepAlive() {
            ScheduledFuture<?> future = keepAlive;
            if (future != null) {
                future.cancel(false);
            }
        }

        @Override
        public void onComplete(AsyncEvent event) {
            getLogger().debug("SSE connection disconnected");
            ended();
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            getLogger().debug("SSE connection timed out");
            ended();
        }

        @Override
        public void onError(AsyncEvent event) {
            getLogger().debug("SSE connection error", event.getThrowable());
            ended();
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            // The listener is registered after the async processing has
            // started
        }
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
     * Sends a single UIDL message to the client and ends the event stream. The
     * message must be sent as an event stream, as {@code EventSource} on the
     * client cannot read any other kind of response.
     *
     * @param response
     *            the response to write to
     * @param json
     *            the UIDL message to send
     * @throws IOException
     *             if writing fails
     */
    private void sendNotification(HttpServletResponse response, String json)
            throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(SsePushConnection.toEvent(json));
        response.getWriter().flush();
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
    public synchronized void destroy() {
        if (keepAliveExecutor != null) {
            keepAliveExecutor.shutdownNow();
            keepAliveExecutor = null;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SseRequestHandler.class.getName());
    }
}
