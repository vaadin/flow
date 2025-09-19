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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import tools.jackson.core.JacksonException;
import org.apache.commons.io.IOUtils;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.SessionExpiredException;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.DauEnforcementException;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.communication.PushMode;

/**
 * Handles incoming push connections and messages and dispatches them to the
 * correct {@link UI}/ {@link AtmospherePushConnection}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PushHandler {

    private int longPollingSuspendTimeout = -1;

    /**
     * Buffer for disconnected resource UUIDs. Key is UUID and value is a
     * timestamp in milliseconds. UUIDs in this map are only needed for short
     * time for handling session expiration.
     */
    private final Map<String, Long> disconnectedUuidBuffer = new ConcurrentHashMap<>();

    /**
     * Callback interface used internally to process an event with the
     * corresponding UI properly locked.
     */
    interface PushEventCallback {

        /**
         * The callback method.
         *
         * @param resource
         *            the Atmosphere resource
         * @param ui
         *            the UI instance
         * @throws IOException
         *             thrown if something goes wrong
         */
        void run(AtmosphereResource resource, UI ui) throws IOException;
    }

    /**
     * Callback used when we receive a request to establish a push channel for a
     * UI. Associate the AtmosphereResource with the UI and leave the connection
     * open by calling resource.suspend(). If there is a pending push, send it
     * now.
     */
    private final PushEventCallback establishCallback = (resource, ui) -> {
        getLogger().debug(
                "New push connection for resource {} with transport {}",
                resource.uuid(), resource.transport());

        resource.getResponse().setContentType("text/plain; charset=UTF-8");

        VaadinSession session = ui.getSession();
        String requestToken = resource.getRequest()
                .getParameter(ApplicationConstants.PUSH_ID_PARAMETER);
        if (!isPushIdValid(session, requestToken)) {
            getLogger().warn(
                    "Invalid identifier in new connection received from {}",
                    resource.getRequest().getRemoteHost());
            // Refresh on client side, create connection just for
            // sending a message
            sendRefreshAndDisconnect(resource);
            return;
        }

        suspend(resource);

        AtmospherePushConnection connection = getConnectionForUI(ui);
        assert (connection != null);
        connection.connect(resource);
    };

    /**
     * Callback used when we receive a UIDL request through Atmosphere. If the
     * push channel is bidirectional (websockets), the request was sent via the
     * same channel. Otherwise, the client used a separate AJAX request. Handle
     * the request and send changed UI state via the push channel (we do not
     * respond to the request directly.)
     */
    private final PushEventCallback receiveCallback = (resource, ui) -> {
        getLogger().debug("Received message from resource {}", resource.uuid());

        AtmosphereRequest req = resource.getRequest();

        AtmospherePushConnection connection = getConnectionForUI(ui);

        assert connection != null : "Got push from the client "
                + "even though the connection does not seem to be "
                + "valid. This might happen if a HttpSession is "
                + "serialized and deserialized while the push "
                + "connection is kept open or if the UI has a "
                + "connection of unexpected type.";

        Reader reader = AtmospherePushConnection.receiveMessage(resource,
                req.getReader(), connection);
        if (reader == null) {
            // The whole message was not yet received
            return;
        }

        // Should be set up by caller
        VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
        assert vaadinRequest != null;

        try {
            new ServerRpcHandler().handleRpc(ui,
                    SynchronizedRequestHandler.getRequestBody(reader),
                    vaadinRequest);
            connection.push(false);
        } catch (JacksonException e) {
            getLogger().error("Error writing JSON to response", e);
            // Refresh on client side
            sendRefreshAndDisconnect(resource);
        } catch (InvalidUIDLSecurityKeyException e) {
            getLogger().warn("Invalid security key received from {}",
                    resource.getRequest().getRemoteHost());
            // Refresh on client side
            sendRefreshAndDisconnect(resource);
        } catch (DauEnforcementException e) {
            getLogger().warn(
                    "Daily Active User limit reached. Blocking new user request");
            String json = DAUUtils.jsonEnforcementResponse(vaadinRequest, e);
            sendNotificationAndDisconnect(resource, json);
        }

    };

    private VaadinServletService service;

    /**
     * Creates an instance connected to the given service.
     *
     * @param service
     *            the service this handler belongs to
     */
    public PushHandler(VaadinServletService service) {
        this.service = service;
    }

    /**
     * Suspends the given resource
     *
     * @param resource
     *            the resource to suspend
     */
    protected void suspend(AtmosphereResource resource) {
        if (resource.transport() == TRANSPORT.LONG_POLLING) {
            resource.suspend(getLongPollingSuspendTimeout());
        } else {
            resource.suspend(-1);
        }
    }

    /**
     * Invokes a command with the current service and session set.
     *
     * @param resource
     *            the atmosphere resource for the current request
     * @param command
     *            the command to run
     */
    void callWithServiceAndSession(final AtmosphereResource resource,
            final Consumer<AtmosphereRequest> command) {
        AtmosphereRequest req = resource.getRequest();
        VaadinServletRequest vaadinRequest = new VaadinServletRequest(req,
                service);
        service.setCurrentInstances(vaadinRequest, null);
        VaadinSession session = null;
        try {
            session = service.findVaadinSession(vaadinRequest);
            VaadinSession.setCurrent(session);
        } catch (SessionExpiredException e) {
            // Call without a session
        }
        try {
            command.accept(req);
        } finally {
            CurrentInstance.clearAll();
        }
    }

    /**
     * Find the UI for the atmosphere resource, lock it and invoke the callback.
     *
     * @param resource
     *            the atmosphere resource for the current request
     * @param callback
     *            the push callback to call when a UI is found and locked
     */
    void callWithUi(final AtmosphereResource resource,
            final PushEventCallback callback) {
        AtmosphereRequest req = resource.getRequest();
        VaadinServletRequest vaadinRequest = new VaadinServletRequest(req,
                service);
        VaadinSession session = null;

        boolean isWebsocket = resource.transport() == TRANSPORT.WEBSOCKET;
        if (isWebsocket) {
            // For any HTTP request we have already started the request in the
            // servlet
            if (callback == receiveCallback) {
                // Allow DAU tracking only for received messages, as websocket
                // connection is not considered a user interaction
                // Executing through TrackableRequest causes no side effects
                // when DAU is not enabled.
                DAUUtils.TrackableOperation.INSTANCE.execute(() -> {
                    service.requestStart(vaadinRequest, null);
                });
            } else {
                service.requestStart(vaadinRequest, null);
            }
        }
        try {
            try {
                session = service.findVaadinSession(vaadinRequest);
                assert VaadinSession.getCurrent() == session;
            } catch (SessionExpiredException e) {
                if (!isResourceDisconnected(resource)) {
                    sendNotificationAndDisconnect(resource,
                            VaadinService.createSessionExpiredJSON(true));
                }
                return;
            }
            cleanDisconnectedUuidBuffer(resource);

            UI ui = null;
            session.lock();
            try {
                ui = service.findUI(vaadinRequest);
                assert UI.getCurrent() == ui;

                if (ui == null) {
                    sendNotificationAndDisconnect(resource,
                            VaadinService.createUINotFoundJSON(true));
                } else {
                    callback.run(resource, ui);
                }
            } catch (final IOException e) {
                callErrorHandler(session, e);
            } catch (final Exception e) {
                SystemMessages msg = service.getSystemMessages(
                        HandlerHelper.findLocale(null, vaadinRequest),
                        vaadinRequest);

                AtmosphereResource errorResource = resource;
                if (ui != null
                        && ui.getInternals().getPushConnection() != null) {
                    // We MUST use the opened push connection if there is one.
                    // Otherwise we will write the response to the wrong request
                    // when using streaming (the client -> server request
                    // instead of the opened push channel)
                    errorResource = ((AtmospherePushConnection) ui
                            .getInternals().getPushConnection()).getResource();
                }

                sendNotificationAndDisconnect(errorResource,
                        VaadinService.createCriticalNotificationJSON(
                                msg.getInternalErrorCaption(),
                                msg.getInternalErrorMessage(), null,
                                msg.getInternalErrorURL()));
                callErrorHandler(session, e);
            } finally {
                try {
                    session.unlock();
                } catch (Exception e) {
                    getLogger().warn("Error while unlocking session", e);
                    // can't call ErrorHandler, we (hopefully) don't have a lock
                }
            }
        } finally {
            try {
                if (isWebsocket) {
                    service.requestEnd(vaadinRequest, null, session);
                }
            } catch (Exception e) {
                getLogger().warn("Error while ending request", e);

                // can't call ErrorHandler, we don't have a lock
            }
        }
    }

    private void cleanDisconnectedUuidBuffer(AtmosphereResource resource) {
        // remove given resource uuid and also all uuid's that were disconnected
        // more than ten seconds ago.
        disconnectedUuidBuffer.remove(resource.uuid());
        Long now = System.currentTimeMillis();
        disconnectedUuidBuffer.entrySet().stream()
                .filter(entry -> (now - entry.getValue()) > 10000)
                .map(Map.Entry::getKey).toList()
                .forEach(disconnectedUuidBuffer::remove);
    }

    private boolean isResourceDisconnected(AtmosphereResource resource) {
        return disconnectedUuidBuffer.containsKey(resource.uuid());
    }

    /**
     * Call the session's error handler.
     */
    private void callErrorHandler(VaadinSession session, Exception e) {
        session.getErrorHandler().error(new ErrorEvent(e));
    }

    private static AtmospherePushConnection getConnectionForUI(UI ui) {
        PushConnection pushConnection = ui.getInternals().getPushConnection();
        if (pushConnection instanceof AtmospherePushConnection) {
            return (AtmospherePushConnection) pushConnection;
        } else {
            return null;
        }
    }

    void connectionLost(AtmosphereResourceEvent event) {
        /*
         * There are two ways being called here: one is from
         * VaadinService:handleRequest (via several interim calls), another is
         * directly from Atmosphere (PushAtmosphereHandler,
         * AtmosphereResourceListener::onDisconnect).
         *
         * In the first case everything will be cleaned up out of the box. In
         * the second case "clear" should be done here otherwise instances will
         * stay in the threads.
         */
        boolean needsClear = VaadinSession.getCurrent() == null;
        try {
            handleConnectionLost(event);
        } finally {
            if (needsClear) {
                CurrentInstance.clearAll();
            }
        }
    }

    private VaadinSession handleConnectionLost(AtmosphereResourceEvent event) {
        if (event == null) {
            getLogger().error("Could not get event. This should never happen.");
            return null;
        }
        // We don't want to use callWithUi here, as it assumes there's a client
        // request active and does requestStart and requestEnd among other
        // things.

        AtmosphereResource resource = event.getResource();
        if (resource == null) {
            return null;
        }

        // In development mode we may have a live-reload push channel
        // that should be closed.

        Optional<BrowserLiveReload> liveReload = Optional.empty();
        try {
            liveReload = BrowserLiveReloadAccessor
                    .getLiveReloadFromService(service);
        } catch (IllegalStateException e) {
            getLogger().debug(
                    "Could not get live-reload push channel to close it.", e);
        }
        if (isDebugWindowConnection(resource) && liveReload.isPresent()
                && liveReload.get().isLiveReload(resource)) {
            liveReload.get().onDisconnect(resource);
            return null;
        }

        disconnectedUuidBuffer.put(resource.uuid(), System.currentTimeMillis());

        VaadinServletRequest vaadinRequest = new VaadinServletRequest(
                resource.getRequest(), service);

        VaadinSession session = null;
        try {
            session = service.findVaadinSession(vaadinRequest);
        } catch (SessionExpiredException e) {
            // This happens at least if the server is restarted without
            // preserving the session. After restart the client reconnects, gets
            // a session expired notification and then closes the connection and
            // ends up here
            getLogger().debug(
                    "Session expired before push disconnect event was received",
                    e);
            return session;
        }

        UI ui;
        session.lock();
        try {
            VaadinSession.setCurrent(session);
            // Sets UI.currentInstance
            ui = service.findUI(vaadinRequest);
            if (ui == null) {
                /*
                 * UI not found, could be because FF has asynchronously closed
                 * the websocket connection and Atmosphere has already done
                 * cleanup of the request attributes.
                 *
                 * In that case, we still have a chance of finding the right UI
                 * by iterating through the UIs in the session looking for one
                 * using the same AtmosphereResource.
                 */
                ui = findUiUsingResource(resource, session.getUIs());

                if (ui == null) {
                    getLogger()
                            .debug("Could not get UI. This should never happen,"
                                    + " except when reloading in Firefox and Chrome -"
                                    + " see http://dev.vaadin.com/ticket/14251.");
                    return session;
                } else {
                    getLogger().info(
                            "No UI was found based on data in the request,"
                                    + " but a slower lookup based on the AtmosphereResource succeeded."
                                    + " See http://dev.vaadin.com/ticket/14251 for more details.");
                }
            }

            PushMode pushMode = ui.getPushConfiguration().getPushMode();
            AtmospherePushConnection pushConnection = getConnectionForUI(ui);

            String id = resource.uuid();

            if (pushConnection == null) {
                getLogger().warn(
                        "Could not find push connection to close: {} with transport {}",
                        id, resource.transport());
            } else {
                if (!pushMode.isEnabled()) {
                    /*
                     * The client is expected to close the connection after push
                     * mode has been set to disabled.
                     */
                    getLogger().debug("Connection closed for resource {}", id);
                } else {
                    /*
                     * Unexpected cancel, e.g. if the user closes the browser
                     * tab.
                     */
                    getLogger().debug(
                            "Connection unexpectedly closed for resource {} with transport {}",
                            id, resource.transport());
                }

                pushConnection.connectionLost();
            }

        } catch (final Exception e) {
            callErrorHandler(session, e);
        } finally {
            try {
                session.unlock();
            } catch (Exception e) {
                getLogger().warn("Error while unlocking session", e);
                // can't call ErrorHandler, we (hopefully) don't have a lock
            }
        }
        return session;
    }

    private static UI findUiUsingResource(AtmosphereResource resource,
            Collection<UI> uIs) {
        for (UI ui : uIs) {
            PushConnection pushConnection = ui.getInternals()
                    .getPushConnection();
            if (pushConnection instanceof AtmospherePushConnection) {
                if (((AtmospherePushConnection) pushConnection)
                        .getResource() == resource) {
                    return ui;
                }
            }
        }
        return null;
    }

    /**
     * Sends a refresh message to the given atmosphere resource. Uses an
     * AtmosphereResource instead of an AtmospherePushConnection even though it
     * might be possible to look up the AtmospherePushConnection from the UI to
     * ensure border cases work correctly, especially when there temporarily are
     * two push connections which try to use the same UI. Using the
     * AtmosphereResource directly guarantees the message goes to the correct
     * recipient.
     *
     * @param resource
     *            The atmosphere resource to send refresh to
     *
     */
    private static void sendRefreshAndDisconnect(AtmosphereResource resource) {
        sendNotificationAndDisconnect(resource, VaadinService
                .createCriticalNotificationJSON(null, null, null, null));
    }

    /**
     * Tries to send a critical notification to the client and close the
     * connection. Does nothing if the connection is already closed.
     */
    private static void sendNotificationAndDisconnect(
            AtmosphereResource resource, String notificationJson) {
        // TODO Implemented differently from sendRefreshAndDisconnect
        try {
            if (resource instanceof AtmosphereResourceImpl
                    && !((AtmosphereResourceImpl) resource).isInScope()) {
                // The resource is no longer valid so we should not write
                // anything to it
                getLogger().debug(
                        "sendNotificationAndDisconnect called for resource no longer in scope");
                return;
            }
            resource.getResponse()
                    .setContentType(JsonConstants.JSON_CONTENT_TYPE);
            resource.getResponse().getWriter().write(notificationJson);
            resource.resume();
        } catch (Exception e) {
            getLogger().trace("Failed to send critical notification to client",
                    e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(PushHandler.class.getName());
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
     * Called when a new push connection is requested to be opened by the client
     *
     * @param resource
     *            The related atmosphere resources
     */
    void onConnect(AtmosphereResource resource) {
        if (isDebugWindowConnection(resource)) {
            if (isProductionMode()) {
                getLogger().debug(
                        "Debug window connection request denied while in production mode");
                // No debug info must ever leak out in production
                return;
            }
            callWithServiceAndSession(resource,
                    request -> BrowserLiveReloadAccessor
                            .getLiveReloadFromService(service)
                            .ifPresent(liveReload -> liveReload
                                    .onConnect(resource)));
        } else {
            LongPollingCacheFilter.onConnect(resource);
            callWithUi(resource, establishCallback);
        }
    }

    private boolean isProductionMode() {
        VaadinContext context = service.getContext();
        ApplicationConfiguration conf = ApplicationConfiguration.get(context);
        return conf.isProductionMode();
    }

    /**
     * Called when a message is received through the push connection
     *
     * @param resource
     *            The related atmosphere resources
     */
    void onMessage(AtmosphereResource resource) {
        if (isDebugWindowConnection(resource)) {
            callWithServiceAndSession(resource, this::handleDebugWindowMessage);
        } else {
            callWithUi(resource, receiveCallback);
        }
    }

    private void handleDebugWindowMessage(AtmosphereRequest request) {
        try {
            Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                    .getLiveReloadFromService(service);
            if (!liveReload.isPresent()) {
                getLogger().error(
                        "Received message for debug window but there is no debug window connection available");
                return;
            }
            AtmosphereResource resource = request.resource();
            Reader reader = AtmospherePushConnection.receiveMessage(resource,
                    request.getReader(), liveReload.get());
            if (reader == null) {
                // The whole message was not yet received
                return;
            }

            String msg = IOUtils.toString(reader);
            liveReload.get().onMessage(resource, msg);
        } catch (IOException e) {
            getLogger().error(
                    "Unable to read contents of debug connection message", e);
        } finally {
            CurrentInstance.clearAll();
        }
    }

    private boolean isDebugWindowConnection(AtmosphereResource resource) {
        String refreshConnection = resource.getRequest()
                .getParameter(ApplicationConstants.DEBUG_WINDOW_CONNECTION);
        return refreshConnection != null
                && TRANSPORT.WEBSOCKET.equals(resource.transport());
    }

    /**
     * Sets the timeout used for suspend calls when using long polling.
     *
     * If you are using a proxy with a defined idle timeout, set the suspend
     * timeout to a value smaller than the proxy timeout so that the server is
     * aware of a reconnect taking place.
     *
     * @param longPollingSuspendTimeout
     *            the timeout to use for suspended AtmosphereResources
     */
    public void setLongPollingSuspendTimeout(int longPollingSuspendTimeout) {
        this.longPollingSuspendTimeout = longPollingSuspendTimeout;
    }

    /**
     * Gets the timeout used for suspend calls when using long polling.
     *
     * @return the timeout to use for suspended AtmosphereResources
     */
    public int getLongPollingSuspendTimeout() {
        return longPollingSuspendTimeout;
    }
}
