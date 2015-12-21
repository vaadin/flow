/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.client.communication;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ApplicationConnection.MultiStepDuration;
import com.vaadin.client.ApplicationConnection.ResponseHandlingStartedEvent;
import com.vaadin.client.ApplicationConnection.State;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.ConnectorMap;
import com.vaadin.client.FastStringSet;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.Profiler;
import com.vaadin.client.Util;
import com.vaadin.client.ValueMap;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.tree.TreeListenerHelper;
import com.vaadin.client.communication.tree.TreeNodeProperty;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;
import com.vaadin.client.communication.tree.TreeUpdater;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.ui.UIConnector;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.communication.PushMode;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * ServerMessageHandler is responsible for handling all incoming messages (JSON)
 * from the server (state changes, RPCs and other updates) and ensuring that the
 * connectors are updated accordingly.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ServerMessageHandler {

    private boolean initial = true;

    /**
     * Helper used to return two values when updating the connector hierarchy.
     */
    private static class ConnectorHierarchyUpdateResult {
        /**
         * Needed at a later point when the created events are fired
         */
        private JsArrayObject<ConnectorHierarchyChangeEvent> events = JavaScriptObject
                .createArray().cast();
        /**
         * Needed to know where captions might need to get updated
         */
        private FastStringSet parentChangedIds = FastStringSet.create();

        /**
         * Connectors for which the parent has been set to null
         */
        private FastStringSet detachedConnectorIds = FastStringSet.create();
    }

    /** The max timeout that response handling may be suspended */
    private static final int MAX_SUSPENDED_TIMEOUT = 5000;

    /**
     * The value of an undefined sync id.
     * <p>
     * This must be <code>-1</code>, because of the contract in
     * {@link #getLastSeenServerSyncId()}
     */
    private static final int UNDEFINED_SYNC_ID = -1;

    /**
     * If responseHandlingLocks contains any objects, response handling is
     * suspended until the collection is empty or a timeout has occurred.
     */
    private Set<Object> responseHandlingLocks = new HashSet<Object>();

    /**
     * Contains all UIDL messages received while response handling is suspended
     */
    private List<PendingUIDLMessage> pendingUIDLMessages = new ArrayList<PendingUIDLMessage>();

    // will hold the CSRF token once received
    private String csrfToken = ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE;

    /** Timer for automatic redirect to SessionExpiredURL */
    private Timer redirectTimer;

    /** redirectTimer scheduling interval in seconds */
    private int sessionExpirationInterval;

    /**
     * Holds the time spent rendering the last request
     */
    protected int lastProcessingTime;

    /**
     * Holds the total time spent rendering requests during the lifetime of the
     * session.
     */
    protected int totalProcessingTime;

    /**
     * Holds the time it took to load the page and render the first view. -2
     * means that this value has not yet been calculated because the first view
     * has not yet been rendered (or that your browser is very fast). -1 means
     * that the browser does not support the performance.timing feature used to
     * get this measurement.
     *
     * Note: also used for tracking whether the first UIDL has been handled
     */
    private int bootstrapTime = 0;

    /**
     * true if state updates are currently being done
     */
    private boolean updatingState = false;

    /**
     * Holds the timing information from the server-side. How much time was
     * spent servicing the last request and how much time has been spent
     * servicing the session so far. These values are always one request behind,
     * since they cannot be measured before the request is finished.
     */
    private ValueMap serverTimingInfo;

    /**
     * Holds the last seen response id given by the server.
     * <p>
     * The server generates a strictly increasing id for each response to each
     * request from the client. This ID is then replayed back to the server on
     * each request. This helps the server in knowing in what state the client
     * is, and compare it to its own state. In short, it helps with concurrent
     * changes between the client and server.
     * <p>
     * Initial value, i.e. no responses received from the server, is
     * {@link #UNDEFINED_SYNC_ID} ({@value #UNDEFINED_SYNC_ID}). This happens
     * between the bootstrap HTML being loaded and the first UI being rendered;
     */
    private int lastSeenServerSyncId = UNDEFINED_SYNC_ID;

    private ApplicationConnection connection;

    private TreeUpdater treeUpdater = GWT.create(TreeUpdater.class);

    /**
     * Data structure holding information about pending UIDL messages.
     */
    private static class PendingUIDLMessage {
        private ValueMap json;

        public PendingUIDLMessage(ValueMap json) {
            this.json = json;
        }

        public ValueMap getJson() {
            return json;
        }
    }

    /**
     * Sets the application connection this queue is connected to
     *
     * @param connection
     *            the application connection this queue is connected to
     */
    public void setConnection(ApplicationConnection connection) {
        this.connection = connection;
        treeUpdater.init(connection.getContainerElement(),
                connection.getServerRpcQueue(), connection.getCurrentClient());

        TreeListenerHelper.addListener(treeUpdater.getRootNode(),
                "pushConfiguration.mode", false,
                new TreeNodePropertyValueChangeListener() {
                    @Override
                    public void changeValue(Object oldValue, Object newValue) {
                        Profiler.enter(
                                "ServerMessageHandler.pushConfiguration.changeValue");
                        String value = (String) newValue;
                        PushMode mode;
                        if (value == null || value.isEmpty()) {
                            mode = PushMode.DEFAULT;
                        } else {
                            mode = PushMode.valueOf(value);
                        }
                        TreeNodeProperty property = treeUpdater.getRootNode()
                                .getProperty("pushConfiguration");
                        ValueMap pushConfig = (ValueMap) property
                                .getProxyValue();
                        connection.getServerCommunicationHandler()
                                .setPushEnabled(mode.isEnabled(), pushConfig);
                        Profiler.enter(
                                "ServerMessageHandler.pushConfiguration.changeValue");
                    }
                });
    }

    public static Logger getLogger() {
        return Logger.getLogger(ServerMessageHandler.class.getName());
    }

    /**
     * Handles a received UIDL JSON text, parsing it, and passing it on to the
     * appropriate handlers, while logging timing information.
     *
     * @param jsonText
     *            The JSON to handle
     */
    public void handleMessage(String jsonText) {
        final Date start = new Date();
        final ValueMap json;
        try {
            json = parseJSONResponse(jsonText);
        } catch (final Exception e) {
            // Should not call endRequest for a asynchronous push message
            // but there is currently no way of knowing if this is an async
            // message if we get invalid JSON.

            // TODO Move parsing out from this method and handle the error the
            // same way as if we do not receive the expected prefix and suffix
            getServerCommunicationHandler().endRequest();

            connection.showCommunicationError(
                    e.getMessage() + " - Original JSON-text:" + jsonText, 200);
            return;
        }
        getLogger().info("JSON parsing took "
                + (new Date().getTime() - start.getTime()) + "ms");
        handleMessage(json);
    }

    private native String formatJson(JsonValue jsonObject, int indent)
    /*-{
        // skip hashCode field
        return $wnd.JSON.stringify(jsonObject, function(keyName, value) {
            if (keyName == "$H") {
              return undefined; // skip hashCode property
            }
            return value;
          }, indent);
      }-*/;

    public void handleMessage(ValueMap json) {

        if (getServerId(json) == -1) {
            getLogger().severe("Response didn't contain a server id. "
                    + "Please verify that the server is up-to-date and that the response data has not been modified in transmission.");
        }

        if (connection.getState() == State.RUNNING) {
            handleJSON(json);
        } else if (connection.getState() == State.INITIALIZING) {
            // Application is starting up for the first time
            connection.setApplicationRunning(true);
            connection.executeWhenDependenciesLoaded(new Command() {
                @Override
                public void execute() {
                    handleJSON(json);
                }
            });
        } else {
            getLogger().warning(
                    "Ignored received message because application has already been stopped");
            return;
        }
    }

    private static native ValueMap parseJSONResponse(String jsonText)
    /*-{
       return JSON.parse(jsonText);
    }-*/;

    protected void handleJSON(final ValueMap json) {
        final int serverId = getServerId(json);
        JsonValue jsonObject = Util.jso2json(json);
        getLogger().info("Handling JSON:\n" + formatJson(jsonObject, 4));

        if (isResynchronize(json) && !isNextExpectedMessage(serverId)) {
            // Resynchronize request. We must remove any old pending
            // messages and ensure this is handled next. Otherwise we
            // would keep waiting for an older message forever (if this
            // is triggered by forceHandleMessage)
            getLogger().info("Received resync message with id " + serverId
                    + " while waiting for " + getExpectedServerId());
            lastSeenServerSyncId = serverId - 1;
            removeOldPendingMessages();
        }

        boolean locked = !responseHandlingLocks.isEmpty();

        if (locked || !isNextExpectedMessage(serverId)) {
            // Cannot or should not handle this message right now, either
            // because of locks or because it's an out-of-order message

            if (locked) {
                // Some component is doing something that can't be interrupted
                // (e.g. animation that should be smooth). Enqueue the UIDL
                // message for later processing.
                getLogger().info("Postponing UIDL handling due to lock...");
            } else {
                // Unexpected server id
                if (serverId <= lastSeenServerSyncId) {
                    // Why is the server re-sending an old package? Ignore it
                    getLogger().warning("Received message with server id "
                            + serverId + " but have already seen "
                            + lastSeenServerSyncId + ". Ignoring it");
                    endRequestIfResponse(json);
                    return;
                }

                // We are waiting for an earlier message...
                getLogger().info("Received message with server id " + serverId
                        + " but expected " + getExpectedServerId()
                        + ". Postponing handling until the missing message(s) have been received");
            }
            pendingUIDLMessages.add(new PendingUIDLMessage(json));
            if (!forceHandleMessage.isRunning()) {
                forceHandleMessage.schedule(MAX_SUSPENDED_TIMEOUT);
            }
            return;
        }

        final Date start = new Date();
        /*
         * Lock response handling to avoid a situation where something pushed
         * from the server gets processed while waiting for e.g. lazily loaded
         * connectors that are needed for processing the current message.
         */
        final Object lock = new Object();
        suspendReponseHandling(lock);

        getLogger().info("Handling message from server");
        connection.fireEvent(new ResponseHandlingStartedEvent(connection));

        // Client id must be updated before server id, as server id update can
        // cause a resync (which must use the updated id)
        if (json.containsKey(ApplicationConstants.CLIENT_TO_SERVER_ID)) {
            int serverNextExpected = json
                    .getInt(ApplicationConstants.CLIENT_TO_SERVER_ID);
            getServerCommunicationHandler().setClientToServerMessageId(
                    serverNextExpected, isResynchronize(json));
        }

        if (serverId != -1) {
            /*
             * Use sync id unless explicitly set as undefined, as is done by
             * e.g. critical server-side notifications
             */
            lastSeenServerSyncId = serverId;
        }

        // Handle redirect
        if (json.containsKey("redirect")) {
            String url = json.getValueMap("redirect").getString("url");
            getLogger().info("redirecting to " + url);
            WidgetUtil.redirect(url);
            return;
        }

        final MultiStepDuration handleUIDLDuration = new MultiStepDuration();

        // Get security key
        if (json.containsKey(ApplicationConstants.UIDL_SECURITY_TOKEN_ID)) {
            csrfToken = json
                    .getString(ApplicationConstants.UIDL_SECURITY_TOKEN_ID);
        }

        getLogger().info("Handling resource dependencies");
        if (json.containsKey("scriptDependencies")) {
            connection.loadScriptDependencies(
                    json.getJSStringArray("scriptDependencies"));
        }
        if (json.containsKey("styleDependencies")) {
            connection.loadStyleDependencies(
                    json.getJSStringArray("styleDependencies"));
        }
        if (json.containsKey("htmlDependencies")) {
            connection.loadHtmlDependencies(
                    json.getJSStringArray("htmlDependencies"));
        }
        if(json.containsKey("polymerStyleDependencies")) {
            connection.loadPolymerStyleDependencies(
                    json.getJSStringArray("polymerStyleDependencies"));
        }

        handleUIDLDuration.logDuration(
                " * Handling type mappings from server completed", 10);
        /*
         * Hook for e.g. TestBench to get details about server peformance
         */
        if (json.containsKey("timings")) {
            serverTimingInfo = json.getValueMap("timings");
        }

        Command c = new Command() {
            private boolean onlyNoLayoutUpdates = true;

            @Override
            public void execute() {
                assert serverId == -1 || serverId == lastSeenServerSyncId;

                handleUIDLDuration.logDuration(" * Loading widgets completed",
                        10);

                Profiler.enter("Handling meta information");
                ValueMap meta = null;
                if (json.containsKey("meta")) {
                    getLogger().info(" * Handling meta information");
                    meta = json.getValueMap("meta");
                    if (meta.containsKey("repaintAll")) {
                        prepareRepaintAll();
                    }
                    if (meta.containsKey("timedRedirect")) {
                        final ValueMap timedRedirect = meta
                                .getValueMap("timedRedirect");
                        if (redirectTimer != null) {
                            redirectTimer.cancel();
                        }
                        redirectTimer = new Timer() {
                            @Override
                            public void run() {
                                WidgetUtil.redirect(
                                        timedRedirect.getString("url"));
                            }
                        };
                        sessionExpirationInterval = timedRedirect
                                .getInt("interval");
                    }
                }
                Profiler.leave("Handling meta information");

                if (redirectTimer != null) {
                    redirectTimer.schedule(1000 * sessionExpirationInterval);
                }

                updatingState = true;

                double processUidlStart = Duration.currentTimeMillis();

                Profiler.enter("Handle element update");

                // Get an instance of a proper JSON api
                JsonObject jsonJson = json.cast();

                treeUpdater.update(jsonJson.getObject("elementTemplates"),
                        jsonJson.getArray("elementChanges"),
                        jsonJson.getArray("rpc"));

                Profiler.leave("Handle element update");

                getLogger().info("handleUIDLMessage: "
                        + (Duration.currentTimeMillis() - processUidlStart)
                        + " ms");

                updatingState = false;

                if (meta != null) {
                    Profiler.enter("Error handling");
                    if (meta.containsKey("appError")) {
                        ValueMap error = meta.getValueMap("appError");

                        VNotification.showError(connection,
                                error.getString("caption"),
                                error.getString("message"),
                                error.getString("details"),
                                error.getString("url"));

                        connection.setApplicationRunning(false);
                    }
                    Profiler.leave("Error handling");
                }

                lastProcessingTime = (int) ((new Date().getTime())
                        - start.getTime());
                totalProcessingTime += lastProcessingTime;
                if (bootstrapTime == 0) {
                    bootstrapTime = calculateBootstrapTime();
                    if (Profiler.isEnabled() && bootstrapTime != -1) {
                        Profiler.logBootstrapTimings();
                    }
                }

                getLogger().info(" Processing time was "
                        + String.valueOf(lastProcessingTime) + "ms");
                getLogger().info(
                        "Referenced paintables: " + getConnectorMap().size());

                endRequestIfResponse(json);
                resumeResponseHandling(lock);

                if (Profiler.isEnabled()) {
                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                        @Override
                        public void execute() {
                            Profiler.logTimings();
                            Profiler.reset();
                        }
                    });
                }

                if (initial) {
                    initial = false;

                    double fetchStart = getPerformanceTiming("fetchStart");
                    if (fetchStart != 0) {
                        int time = (int) (Duration.currentTimeMillis()
                                - fetchStart);
                        getLogger().log(Level.INFO, "First response processed "
                                + time + " ms after fetchStart");
                    }
                }
            }

            /**
             * Properly clean up any old stuff to ensure everything is properly
             * reinitialized.
             */
            private void prepareRepaintAll() {
                // FIXME
            }

        };
        ApplicationConfiguration.runWhenDependenciesLoaded(c);
    }

    public static final native double getPerformanceTiming(String name)
    /*-{
        if ($wnd.performance && $wnd.performance.timing && $wnd.performance.timing[name]) {
            return $wnd.performance.timing[name];
        } else {
            return 0;
        }
    }-*/;

    private void endRequestIfResponse(ValueMap json) {
        if (isResponse(json)) {
            // End the request if the received message was a
            // response, not sent asynchronously
            getServerCommunicationHandler().endRequest();
        }
    }

    private boolean isResynchronize(ValueMap json) {
        return json.containsKey(ApplicationConstants.RESYNCHRONIZE_ID);
    }

    private boolean isResponse(ValueMap json) {
        ValueMap meta = json.getValueMap("meta");
        if (meta == null || !meta.containsKey("async")) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the given serverId is the one we are currently waiting for from
     * the server
     */
    private boolean isNextExpectedMessage(int serverId) {
        if (serverId == -1) {
            return true;
        }
        if (serverId == getExpectedServerId()) {
            return true;
        }
        if (lastSeenServerSyncId == UNDEFINED_SYNC_ID) {
            // First message is always ok
            return true;
        }
        return false;

    }

    private int getServerId(ValueMap json) {
        if (json.containsKey(ApplicationConstants.SERVER_SYNC_ID)) {
            return json.getInt(ApplicationConstants.SERVER_SYNC_ID);
        } else {
            return -1;
        }
    }

    private int getExpectedServerId() {
        return lastSeenServerSyncId + 1;
    }

    /**
     * Timer used to make sure that no misbehaving components can delay response
     * handling forever.
     */
    Timer forceHandleMessage = new Timer() {
        @Override
        public void run() {
            if (!responseHandlingLocks.isEmpty()) {
                // Lock which was never release -> bug in locker or things just
                // too slow
                getLogger().warning(
                        "WARNING: reponse handling was never resumed, forcibly removing locks...");
                responseHandlingLocks.clear();
            } else {
                // Waited for out-of-order message which never arrived
                // Do one final check and resynchronize if the message is not
                // there. The final check is only a precaution as this timer
                // should have been cancelled if the message has arrived
                getLogger().warning("Gave up waiting for message "
                        + getExpectedServerId() + " from the server");

            }
            if (!handlePendingMessages() && !pendingUIDLMessages.isEmpty()) {
                // There are messages but the next id was not found, likely it
                // has been lost
                // Drop pending messages and resynchronize
                pendingUIDLMessages.clear();
                getServerCommunicationHandler().resynchronize();
            }
        }
    };

    /**
     * This method can be used to postpone rendering of a response for a short
     * period of time (e.g. to avoid the rendering process during animation).
     *
     * @param lock
     */
    public void suspendReponseHandling(Object lock) {
        responseHandlingLocks.add(lock);
    }

    /**
     * Resumes the rendering process once all locks have been removed.
     *
     * @param lock
     */
    public void resumeResponseHandling(Object lock) {
        responseHandlingLocks.remove(lock);
        if (responseHandlingLocks.isEmpty()) {
            // Cancel timer that breaks the lock
            forceHandleMessage.cancel();

            if (!pendingUIDLMessages.isEmpty()) {
                getLogger().info(
                        "No more response handling locks, handling pending requests.");
                handlePendingMessages();
            }
        }
    }

    private static native final int calculateBootstrapTime()
    /*-{
        if ($wnd.performance && $wnd.performance.timing) {
            return (new Date).getTime() - $wnd.performance.timing.responseStart;
        } else {
            // performance.timing not supported
            return -1;
        }
    }-*/;

    /**
     * Finds the next pending UIDL message and handles it (next pending is
     * decided based on the server id)
     *
     * @return true if a message was handled, false otherwise
     */
    private boolean handlePendingMessages() {
        if (pendingUIDLMessages.isEmpty()) {
            return false;
        }

        // Try to find the next expected message
        PendingUIDLMessage toHandle = null;
        for (PendingUIDLMessage message : pendingUIDLMessages) {
            if (isNextExpectedMessage(getServerId(message.json))) {
                toHandle = message;
                break;
            }
        }

        if (toHandle != null) {
            pendingUIDLMessages.remove(toHandle);
            handleJSON(toHandle.getJson());
            // Any remaining messages will be handled when this is called
            // again at the end of handleJSON
            return true;
        } else {
            return false;
        }

    }

    private void removeOldPendingMessages() {
        Iterator<PendingUIDLMessage> i = pendingUIDLMessages.iterator();
        while (i.hasNext()) {
            PendingUIDLMessage m = i.next();
            int serverId = getServerId(m.json);
            if (serverId != -1 && serverId < getExpectedServerId()) {
                getLogger().info("Removing old message with id " + serverId);
                i.remove();
            }
        }
    }

    /**
     * Gets the server id included in the last received response.
     * <p>
     * This id can be used by connectors to determine whether new data has been
     * received from the server to avoid doing the same calculations multiple
     * times.
     * <p>
     * No guarantees are made for the structure of the id other than that there
     * will be a new unique value every time a new response with data from the
     * server is received.
     * <p>
     * The initial id when no request has yet been processed is -1.
     *
     * @return an id identifying the response
     */
    public int getLastSeenServerSyncId() {
        return lastSeenServerSyncId;
    }

    /**
     * Gets the token (aka double submit cookie) that the server uses to protect
     * against Cross Site Request Forgery attacks.
     *
     * @return the CSRF token string
     */
    public String getCsrfToken() {
        return csrfToken;
    }

    /**
     * Checks whether state changes are currently being processed. Certain
     * operations are not allowed when the internal state of the application
     * might be in an inconsistent state because some state changes have been
     * applied but others not. This includes running layotus.
     *
     * @return <code>true</code> if the internal state might be inconsistent
     *         because changes are being processed; <code>false</code> if the
     *         state should be consistent
     */
    public boolean isUpdatingState() {
        return updatingState;
    }

    /**
     * Checks if the first UIDL has been handled
     *
     * @return true if the initial UIDL has already been processed, false
     *         otherwise
     */
    public boolean isInitialUidlHandled() {
        return bootstrapTime != 0;
    }

    private ConnectorMap getConnectorMap() {
        return ConnectorMap.get(connection);
    }

    private UIConnector getUIConnector() {
        return connection.getUIConnector();
    }

    private ServerCommunicationHandler getServerCommunicationHandler() {
        return connection.getServerCommunicationHandler();
    }

    public TreeUpdater getTreeUpdater() {
        return treeUpdater;
    }

}
