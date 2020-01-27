/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;

import com.vaadin.client.Command;
import com.vaadin.client.Console;
import com.vaadin.client.DependencyLoader;
import com.vaadin.client.Profiler;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle.UIState;
import com.vaadin.client.ValueMap;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.TreeChangeProcessor;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.dom.Node;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * A MessageHandler is responsible for handling all incoming messages (JSON)
 * from the server (state changes, RPCs and other updates) and ensuring that the
 * connectors are updated accordingly.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MessageHandler {

    public static final String JSON_COMMUNICATION_PREFIX = "for(;;);[";
    public static final String JSON_COMMUNICATION_SUFFIX = "]";

    /** The max timeout that response handling may be suspended. */
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
    private JsSet<Object> responseHandlingLocks = JsCollections.set();

    /**
     * Contains all UIDL messages received while response handling is suspended.
     */
    private JsArray<PendingUIDLMessage> pendingUIDLMessages = JsCollections
            .array();

    // will hold the CSRF token once received
    private String csrfToken = ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE;

    // holds the push identifier once received
    private String pushId = null;

    /**
     * Holds the time spent rendering the last request.
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
    private int bootstrapTime;

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
    private final Registry registry;

    private boolean initialMessageHandled;
    
    private boolean resyncInProgress;

    /**
     * Timer used to make sure that no misbehaving components can delay response
     * handling forever.
     */
    private Timer forceHandleMessage = new Timer() {
        @Override
        public void run() {
            forceMessageHandling();
        }
    };
    private Command nextResponseSessionExpiredHandler;

    /**
     * Data structure holding information about pending UIDL messages.
     */
    private static class PendingUIDLMessage {
        private ValueMap json;

        /**
         * Creates a new instance based on the given JSON.
         *
         * @param json
         *            the JSON to wrap
         */
        public PendingUIDLMessage(ValueMap json) {
            this.json = json;
        }

        public ValueMap getJson() {
            return json;
        }
    }

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public MessageHandler(Registry registry) {
        this.registry = registry;
    }

    /**
     * Handles a received UIDL JSON text, parsing it, and passing it on to the
     * appropriate handlers, while logging timing information.
     *
     * @param json
     *            The JSON to handle
     */
    public void handleMessage(final ValueMap json) {
        if (json == null) {
            throw new IllegalArgumentException(
                    "The json to handle cannot be null");
        }
        if (getServerId(json) == -1) {

            ValueMap meta = json.getValueMap("meta");

            // Log the error only if session didn't expire.
            if (meta == null
                    || !meta.containsKey(JsonConstants.META_SESSION_EXPIRED)) {
                Console.error("Response didn't contain a server id. "
                        + "Please verify that the server is up-to-date and that the response data has not been modified in transmission.");
            }
        }

        UIState state = registry.getUILifecycle().getState();
        if (state == UIState.INITIALIZING) {
            // Application is starting up for the first time
            state = UIState.RUNNING;
            registry.getUILifecycle().setState(state);
        }

        if (state == UIState.RUNNING) {
            handleJSON(json);
        } else {
            Console.warn(
                    "Ignored received message because application has already been stopped");
        }
    }

    protected void handleJSON(final ValueMap valueMap) {
        final int serverId = getServerId(valueMap);

        boolean hasResynchronize = isResynchronize(valueMap);

        if (!hasResynchronize && resyncInProgress) {
            Console.warn("Dropping the response of a request before a resync request.");
            return;
        }

        resyncInProgress = false;

        if (hasResynchronize && !isNextExpectedMessage(serverId)) {
            // Resynchronize request. We must remove any old pending
            // messages and ensure this is handled next. Otherwise we
            // would keep waiting for an older message forever (if this
            // is triggered by forceHandleMessage)
            Console.log("Received resync message with id " + serverId
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
                Console.log("Postponing UIDL handling due to lock...");
            } else {
                // Unexpected server id
                if (serverId <= lastSeenServerSyncId) {
                    // Why is the server re-sending an old package? Ignore it
                    Console.warn("Received message with server id " + serverId
                            + " but have already seen " + lastSeenServerSyncId
                            + ". Ignoring it");
                    endRequestIfResponse(valueMap);
                    return;
                }

                // We are waiting for an earlier message...
                Console.log("Received message with server id " + serverId
                        + " but expected " + getExpectedServerId()
                        + ". Postponing handling until the missing message(s) have been received");
            }
            pendingUIDLMessages.push(new PendingUIDLMessage(valueMap));
            if (!forceHandleMessage.isRunning()) {
                forceHandleMessage.schedule(MAX_SUSPENDED_TIMEOUT);
            }
            return;
        }

        double start = Duration.currentTimeMillis();
        /*
         * Lock response handling to avoid a situation where something pushed
         * from the server gets processed while waiting for e.g. lazily loaded
         * connectors that are needed for processing the current message.
         */
        final Object lock = new Object();
        suspendReponseHandling(lock);

        Console.log("Handling message from server");
        registry.getRequestResponseTracker()
                .fireEvent(new ResponseHandlingStartedEvent());
        // Client id must be updated before server id, as server id update can
        // cause a resync (which must use the updated id)
        if (valueMap.containsKey(ApplicationConstants.CLIENT_TO_SERVER_ID)) {
            int serverNextExpected = valueMap
                    .getInt(ApplicationConstants.CLIENT_TO_SERVER_ID);
            registry.getMessageSender().setClientToServerMessageId(
                    serverNextExpected, hasResynchronize);
        }

        if (serverId != -1) {
            /*
             * Use sync id unless explicitly set as undefined, as is done by
             * e.g. critical server-side notifications
             */
            lastSeenServerSyncId = serverId;
        }

        // Handle redirect
        if (valueMap.containsKey("redirect")) {
            String url = valueMap.getValueMap("redirect").getString("url");
            Console.log("redirecting to " + url);
            WidgetUtil.redirect(url);
            return;
        }

        // Get security key
        if (valueMap.containsKey(ApplicationConstants.UIDL_SECURITY_TOKEN_ID)) {
            csrfToken = valueMap
                    .getString(ApplicationConstants.UIDL_SECURITY_TOKEN_ID);
        }

        // Get push id if present
        if (valueMap.containsKey(ApplicationConstants.UIDL_PUSH_ID)) {
            pushId = valueMap.getString(ApplicationConstants.UIDL_PUSH_ID);
        }

        handleDependencies(valueMap.cast());

        if (!initialMessageHandled) {
            /*
             * When handling the initial JSON message, dependencies are embedded
             * in the HTML document instead of being injected by
             * DependencyLoader. We must still explicitly wait for all HTML
             * imports from the HTML document to be loaded. It's not necessary
             * to explicitly wait for JavaScript dependencies since the browser
             * already takes care of that for us.
             */
            registry.getDependencyLoader().requireHtmlImportsReady();
        }

        /*
         * Hook for e.g. TestBench to get details about server performance
         */
        if (valueMap.containsKey("timings")) {
            serverTimingInfo = valueMap.getValueMap("timings");
        }

        DependencyLoader.runWhenEagerDependenciesLoaded(
                DomApi::updateApiImplementation);
        DependencyLoader.runWhenEagerDependenciesLoaded(
                () -> processMessage(valueMap, lock, start));
    }

    private void handleDependencies(JsonObject inputJson) {
        Console.log("Handling dependencies");
        JsMap<LoadMode, JsonArray> dependencies = JsCollections.map();
        for (LoadMode loadMode : LoadMode.values()) {
            if (inputJson.hasKey(loadMode.name())) {
                dependencies.set(loadMode, inputJson.getArray(loadMode.name()));
            }
        }

        if (!dependencies.isEmpty()) {
            registry.getDependencyLoader().loadDependencies(dependencies);
        }
    }

    /**
     * Performs the actual processing of a server message when all dependencies
     * have been loaded.
     *
     * @param valueMap
     *            the message payload
     * @param lock
     *            the lock object for this response
     * @param start
     *            the time stamp when processing started
     */
    private void processMessage(ValueMap valueMap, Object lock, double start) {
        assert getServerId(valueMap) == -1
                || getServerId(valueMap) == lastSeenServerSyncId;

        try {
            double processUidlStart = Duration.currentTimeMillis();

            JsonObject json = valueMap.cast();

            if (json.hasKey("constants")) {
                ConstantPool constantPool = registry.getConstantPool();
                JsonObject constants = json.getObject("constants");
                constantPool.importFromJson(constants);
            }

            if (json.hasKey("changes")) {
                processChanges(json);
            }

            if (json.hasKey(JsonConstants.UIDL_KEY_EXECUTE)) {
                // Invoke JS only after all tree changes have been
                // propagated and after post flush listeners added during
                // message processing (so add one more post flush listener which
                // is called after all added post listeners).
                Reactive.addPostFlushListener(
                        () -> Reactive.addPostFlushListener(() -> registry
                                .getExecuteJavaScriptProcessor()
                                .execute(json.getArray(
                                        JsonConstants.UIDL_KEY_EXECUTE))));
            }

            Console.log("handleUIDLMessage: "
                    + (Duration.currentTimeMillis() - processUidlStart)
                    + " ms");

            ValueMap meta = valueMap.getValueMap("meta");

            if (meta != null) {
                Profiler.enter("Error handling");
                if (meta.containsKey(JsonConstants.META_SESSION_EXPIRED)) {
                    if (nextResponseSessionExpiredHandler != null) {
                        nextResponseSessionExpiredHandler.execute();
                    } else {
                        registry.getSystemErrorHandler()
                                .handleSessionExpiredError(null);
                        registry.getUILifecycle().setState(UIState.TERMINATED);
                    }
                } else if (meta.containsKey("appError")) {
                    ValueMap error = meta.getValueMap("appError");

                    registry.getSystemErrorHandler().handleUnrecoverableError(
                            error.getString("caption"),
                            error.getString("message"),
                            error.getString("details"), error.getString("url"));

                    registry.getUILifecycle().setState(UIState.TERMINATED);
                }
                Profiler.leave("Error handling");
            }
            nextResponseSessionExpiredHandler = null;
            Reactive.flush();

            lastProcessingTime = (int) (Duration.currentTimeMillis() - start);
            totalProcessingTime += lastProcessingTime;
            if (!initialMessageHandled) {
                initialMessageHandled = true;

                double fetchStart = getFetchStartTime();
                if (fetchStart != 0) {
                    int time = (int) (Duration.currentTimeMillis()
                            - fetchStart);
                    Console.log("First response processed " + time
                            + " ms after fetchStart");
                }

                bootstrapTime = calculateBootstrapTime();
                if (Profiler.isEnabled() && bootstrapTime != -1) {
                    Profiler.logBootstrapTimings();
                }
            }

        } finally {
            Console.log(" Processing time was "
                    + String.valueOf(lastProcessingTime) + "ms");

            endRequestIfResponse(valueMap);
            resumeResponseHandling(lock);

            if (Profiler.isEnabled()) {
                Scheduler.get().scheduleDeferred(() -> {
                    Profiler.logTimings();
                    Profiler.reset();
                });
            }
        }

    }

    private void processChanges(JsonObject json) {
        StateTree tree = registry.getStateTree();
        JsSet<StateNode> updatedNodes = TreeChangeProcessor.processChanges(tree,
                json.getArray("changes"));

        if (!registry.getApplicationConfiguration().isProductionMode()) {
            try {
                JsonObject debugJson = tree.getRootNode().getDebugJson();
                Console.log("StateTree after applying changes:");
                Console.log(debugJson);
            } catch (Exception e) {
                Console.error("Failed to log state tree");
                Console.error(e);
            }
        }

        Reactive.addPostFlushListener(() -> Scheduler.get().scheduleDeferred(
                () -> updatedNodes.forEach(this::afterServerUpdates)));
    }

    private void afterServerUpdates(StateNode node) {
        if (!node.isUnregistered()) {
            callAfterServerUpdates(node.getDomNode());
        }
    }

    private native void callAfterServerUpdates(Node node)
    /*-{
        if ( node && node.afterServerUpdate ) {
            node.afterServerUpdate();
        }
    }-*/;

    private void endRequestIfResponse(ValueMap json) {
        if (isResponse(json)) {
            // End the request if the received message was a
            // response, not sent asynchronously
            registry.getRequestResponseTracker().endRequest();
        }
    }

    private boolean isResynchronize(ValueMap json) {
        return json.containsKey(ApplicationConstants.RESYNCHRONIZE_ID);
    }

    private boolean isResponse(ValueMap json) {
        ValueMap meta = json.getValueMap("meta");
        if (meta == null || !meta.containsKey(JsonConstants.META_ASYNC)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the given serverId is the one we are currently waiting for from
     * the server.
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

    private void forceMessageHandling() {
        // Clear previous request if it exists. Otherwise resyncrhonize can trigger
        // "Trying to start a new request while another is active" exception and fail.
        if (registry.getRequestResponseTracker().hasActiveRequest()) {
            registry.getRequestResponseTracker().endRequest();
        }
        if (!responseHandlingLocks.isEmpty()) {
            // Lock which was never release -> bug in locker or things just
            // too slow
            Console.warn(
                    "WARNING: reponse handling was never resumed, forcibly removing locks...");
            responseHandlingLocks.clear();
        } else {
            // Waited for out-of-order message which never arrived
            // Do one final check and resynchronize if the message is not
            // there. The final check is only a precaution as this timer
            // should have been cancelled if the message has arrived
            Console.warn("Gave up waiting for message " + getExpectedServerId()
                    + " from the server");

        }
        if (!handlePendingMessages() && !pendingUIDLMessages.isEmpty()) {
            // There are messages but the next id was not found, likely it
            // has been lost
            // Drop pending messages and resynchronize
            pendingUIDLMessages.clear();
            registry.getMessageSender().resynchronize();
        }
    }

    /**
     * This method can be used to postpone rendering of a response for a short
     * period of time (e.g. to avoid the rendering process during animation).
     *
     * @param lock
     *            the lock
     */
    public void suspendReponseHandling(Object lock) {
        responseHandlingLocks.add(lock);
    }

    /**
     * Resumes the rendering process once all locks have been removed.
     *
     * @param lock
     *            the lock
     */
    public void resumeResponseHandling(Object lock) {
        responseHandlingLocks.delete(lock);
        if (responseHandlingLocks.isEmpty()) {
            // Cancel timer that breaks the lock
            forceHandleMessage.cancel();

            if (!pendingUIDLMessages.isEmpty()) {
                Console.log(
                        "No more response handling locks, handling pending requests.");
                handlePendingMessages();
            }
        }
    }

    private static final native int calculateBootstrapTime()
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
     * decided based on the server id).
     *
     * @return true if a message was handled, false otherwise
     */
    private boolean handlePendingMessages() {
        if (pendingUIDLMessages.isEmpty()) {
            return false;
        }

        // Try to find the next expected message
        int toHandle = -1;
        for (int i = 0; i < pendingUIDLMessages.length(); i++) {
            PendingUIDLMessage message = pendingUIDLMessages.get(i);
            if (isNextExpectedMessage(getServerId(message.json))) {
                toHandle = i;
                break;
            }
        }

        if (toHandle != -1) {
            PendingUIDLMessage messageToHandle = pendingUIDLMessages
                    .remove(toHandle);
            handleJSON(messageToHandle.getJson());
            // Any remaining messages will be handled when this is called
            // again at the end of handleJSON
            return true;
        } else {
            return false;
        }

    }

    private void removeOldPendingMessages() {
        for (int i = 0; i < pendingUIDLMessages.length(); i++) {
            PendingUIDLMessage m = pendingUIDLMessages.get(i);
            int serverId = getServerId(m.json);
            if (serverId != -1 && serverId < getExpectedServerId()) {
                Console.log("Removing old message with id " + serverId);

                pendingUIDLMessages.remove(i);
                i--;
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
     * Gets the push connection identifier for this session. Used when
     * establishing a push connection with the client.
     *
     * @return the push connection identifier string
     */
    public String getPushId() {
        return pushId;
    }

    /**
     * Checks if the first UIDL has been handled.
     *
     * @return true if the initial UIDL has already been processed, false
     *         otherwise
     */
    public boolean isInitialUidlHandled() {
        return bootstrapTime != 0;
    }

    /**
     * Strips the JSON wrapping from the given json string with wrapping.
     *
     * If the given string is not wrapped as expected, returns null
     *
     * @param jsonWithWrapping
     *            the JSON received from the server
     * @return an unwrapped JSON string or null if the given string was not
     *         wrapped
     */
    public static String stripJSONWrapping(String jsonWithWrapping) {
        if (jsonWithWrapping == null) {
            return null;
        }

        if (!jsonWithWrapping.startsWith(JSON_COMMUNICATION_PREFIX)
                || !jsonWithWrapping.endsWith(JSON_COMMUNICATION_SUFFIX)) {
            return null;
        }
        return jsonWithWrapping.substring(JSON_COMMUNICATION_PREFIX.length(),
                jsonWithWrapping.length() - JSON_COMMUNICATION_SUFFIX.length());
    }

    /**
     * Unwraps and parses the given JSON, originating from the server.
     *
     * @param jsonText
     *            the json from the server
     * @return A parsed ValueMap or null if the input could not be parsed (or
     *         was null)
     */
    public static ValueMap parseJson(String jsonText) {
        if (jsonText == null) {
            return null;
        }
        final double start = Profiler.getRelativeTimeMillis();
        try {
            ValueMap json = parseJSONResponse(jsonText);
            Console.log("JSON parsing took "
                    + Profiler.getRelativeTimeString(start) + "ms");
            return json;
        } catch (final Exception e) {
            Console.error("Unable to parse JSON: " + jsonText);
            return null;
        }
    }

    private static native ValueMap parseJSONResponse(String jsonText)
    /*-{
       return JSON.parse(jsonText);
    }-*/;

    /**
     * Parse the given wrapped JSON, received from the server, to a ValueMap.
     *
     * @param wrappedJsonText
     *            the json, wrapped as done by the server
     * @return a ValueMap, or null if the wrapping was incorrect or json could
     *         not be parsed
     */
    public static ValueMap parseWrappedJson(String wrappedJsonText) {
        return parseJson(stripJSONWrapping(wrappedJsonText));
    }

    private static final native double getFetchStartTime()
    /*-{
        if ($wnd.performance && $wnd.performance.timing && $wnd.performance.timing.fetchStart) {
            return $wnd.performance.timing.fetchStart;
        } else {
            return 0;
        }
    }-*/;

    /**
     * Sets a temporary handler for session expiration. This handler will be
     * triggered if and only if the next server message tells that the session
     * has expired.
     *
     * @param nextResponseSessionExpiredHandler
     *            the handler to use or null to remove a previously set handler
     */
    public void setNextResponseSessionExpiredHandler(
            Command nextResponseSessionExpiredHandler) {
        this.nextResponseSessionExpiredHandler = nextResponseSessionExpiredHandler;
    }

    public void onResynchronize() {
        resyncInProgress = true;
    }
}
