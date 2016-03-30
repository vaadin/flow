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
package com.vaadin.ui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.vaadin.hummingbird.StateTree;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;
import com.vaadin.hummingbird.namespace.DependencyListNamespace;
import com.vaadin.hummingbird.namespace.LoadingIndicatorConfigurationNamespace;
import com.vaadin.hummingbird.namespace.Namespace;
import com.vaadin.hummingbird.namespace.PollConfigurationNamespace;
import com.vaadin.hummingbird.namespace.PushConfigurationMap;
import com.vaadin.hummingbird.namespace.ReconnectDialogConfigurationNamespace;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.Location;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.util.SerializableJson;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.PushConnection;
import com.vaadin.ui.Page.ExecutionCanceler;

/**
 * Holds UI-specific data that is mainly intended for internal use by the
 * framework. API for accessing this data is located in this class to reduce the
 * clutter in the API of the UI class.
 *
 * @author Vaadin Ltd
 * @since
 */
public class FrameworkData implements Serializable {

    /**
     * A {@link Page#executeJavaScript(String, Object...)} invocation that has
     * not yet been sent to the client.
     */
    public static class JavaScriptInvocation implements Serializable {

        private final String expression;
        private transient List<Object> parameters;

        /**
         * Creates a new invocation.
         *
         * @param expression
         *            the expression to invoke
         * @param parameters
         *            a list of parameters to use when invoking the script
         */
        public JavaScriptInvocation(String expression,
                List<Object> parameters) {
            this.expression = expression;
            this.parameters = parameters;
        }

        /**
         * Gets the JavaScript expression to invoke.
         *
         * @return the JavaScript expression
         */
        public String getExpression() {
            return expression;
        }

        /**
         * Gets the parameters to use when invoking the script.
         *
         * @return a list of parameters to use
         */
        public List<Object> getParameters() {
            return Collections.unmodifiableList(parameters);
        }

        @SuppressWarnings("unchecked")
        private void readObject(ObjectInputStream stream)
                throws IOException, ClassNotFoundException {
            stream.defaultReadObject();

            parameters = (List<Object>) stream.readObject();
            SerializableJson.unwrapList(parameters);
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();

            stream.writeObject(
                    SerializableJson.createSerializableList(parameters));
        }
    }

    /**
     * Tracks which message from the client should come next. First message from
     * the client has id 0.
     */
    private int lastProcessedClientToServerId = -1;

    private int serverSyncId = 0;

    private final StateTree stateTree = new StateTree(getRootNodeNamespaces());

    private PushConnection pushConnection = null;

    /**
     * The id of this UI, used to find the server side instance of the UI form
     * which a request originates. A negative value indicates that the UI id has
     * not yet been assigned by the Application.
     *
     * @see VaadinSession#getNextUIid()
     */
    private int uiId = -1;

    /**
     * Timestamp keeping track of the last heartbeat of this UI. Updated to the
     * current time whenever the application receives a heartbeat or UIDL
     * request from the client for this UI.
     */
    private long lastHeartbeatTimestamp = System.currentTimeMillis();

    private List<JavaScriptInvocation> pendingJsInvocations = new ArrayList<>();

    private final UI ui;

    private String title;

    private ExecutionCanceler pendingTitleUpdateCanceler;

    private Location viewLocation = new Location("");
    private ArrayList<View> viewChain = new ArrayList<>();

    /**
     * Creates a new framework data instance for the given UI.
     *
     * @param ui
     *            the UI to use
     */
    public FrameworkData(UI ui) {
        this.ui = ui;
    }

    /**
     * Gets the state tree of this UI.
     *
     * @return the state tree
     */
    public StateTree getStateTree() {
        return stateTree;
    }

    /**
     * Gets the last processed server message id.
     *
     * Used internally for communication tracking.
     *
     * @return lastProcessedServerMessageId the id of the last processed server
     *         message
     */
    public int getLastProcessedClientToServerId() {
        return lastProcessedClientToServerId;
    }

    /**
     * Sets the last processed server message id.
     *
     * Used internally for communication tracking.
     *
     * @param lastProcessedClientToServerId
     *            the id of the last processed server message
     */
    public void setLastProcessedClientToServerId(
            int lastProcessedClientToServerId) {
        this.lastProcessedClientToServerId = lastProcessedClientToServerId;
    }

    /**
     * Gets the server sync id.
     * <p>
     * The sync id is incremented by one whenever a new response is written.
     * This id is then sent over to the client. The client then adds the most
     * recent sync id to each communication packet it sends back to the server.
     * This way, the server knows at what state the client is when the packet is
     * sent. If the state has changed on the server side since that, the server
     * can try to adjust the way it handles the actions from the client side.
     * <p>
     * The sync id value <code>-1</code> is ignored to facilitate testing with
     * pre-recorded requests.
     *
     * @return the server sync id
     */
    public int getServerSyncId() {
        return serverSyncId;
    }

    /**
     * Increments the server sync id.
     * <p>
     * This should only be called by whoever sends a message to the client,
     * after the message has been sent.
     */
    public void incrementServerId() {
        serverSyncId++;
    }

    /**
     * Returns the timestamp of the last received heartbeat for this UI.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return this UI.
     *
     * @see VaadinService#closeInactiveUIs(VaadinSession)
     *
     * @return The time the last heartbeat request occurred, in milliseconds
     *         since the epoch.
     */
    public long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp;
    }

    /**
     * Sets the last heartbeat request timestamp for this UI. Called by the
     * framework whenever the application receives a valid heartbeat request for
     * this UI.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return this UI.
     *
     * @param lastHeartbeat
     *            The time the last heartbeat request occurred, in milliseconds
     *            since the epoch.
     */
    public void setLastHeartbeatTimestamp(long lastHeartbeat) {
        lastHeartbeatTimestamp = lastHeartbeat;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Namespace>[] getRootNodeNamespaces() {
        // Start with all element namespaces
        ArrayList<Class<? extends Namespace>> namespaces = new ArrayList<>(
                BasicElementStateProvider.getNamespaces());

        // Then add our own custom namespaces
        namespaces.add(PushConfigurationMap.class);
        namespaces.add(PollConfigurationNamespace.class);
        namespaces.add(ReconnectDialogConfigurationNamespace.class);
        namespaces.add(LoadingIndicatorConfigurationNamespace.class);
        namespaces.add(DependencyListNamespace.class);

        // And return them all
        assert namespaces.size() == new HashSet<>(namespaces)
                .size() : "There are duplicates";
        return (Class<? extends Namespace>[]) namespaces
                .toArray(new Class<?>[0]);
    }

    /**
     * Gets the id of the UI, used to identify this UI within its application
     * when processing requests. The UI id should be present in every request to
     * the server that originates from this UI.
     * {@link VaadinService#findUI(VaadinRequest)} uses this id to find the
     * route to which the request belongs.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return this UI.
     *
     * @return the id of this UI
     */
    public int getUIId() {
        return uiId;
    }

    /**
     * Returns the internal push connection object used by this UI. This method
     * should only be called by the framework.
     * <p>
     * This method is not intended to be overridden. If it is overridden, care
     * should be taken since this method might be called in situations where
     * {@link UI#getCurrent()} does not return this UI.
     *
     * @return the push connection used by this UI, or {@code null} if push is
     *         not available.
     */
    public PushConnection getPushConnection() {
        assert !(ui.getPushConfiguration().getPushMode().isEnabled()
                && pushConnection == null);
        return pushConnection;
    }

    /**
     * Sets the internal push connection object used by this UI. This method
     * should only be called by the framework.
     * <p>
     * The {@code pushConnection} argument must be non-null if and only if
     * {@code getPushConfiguration().getPushMode().isEnabled()}.
     *
     * @param pushConnection
     *            the push connection to use for this UI
     */
    public void setPushConnection(PushConnection pushConnection) {
        // If pushMode is disabled then there should never be a pushConnection;
        // if enabled there should always be
        assert (pushConnection == null)
                ^ ui.getPushConfiguration().getPushMode().isEnabled();

        if (pushConnection == this.pushConnection) {
            return;
        }

        if (this.pushConnection != null && this.pushConnection.isConnected()) {
            this.pushConnection.disconnect();
        }

        this.pushConnection = pushConnection;
    }

    /**
     * Adds a JavaScript invocation to be sent to the client.
     *
     * @param invocation
     *            the invocation to add
     * @return a callback for canceling the execution if not yet sent to browser
     */
    public ExecutionCanceler addJavaScriptInvocation(
            JavaScriptInvocation invocation) {
        pendingJsInvocations.add(invocation);
        return () -> pendingJsInvocations.remove(invocation);
    }

    /**
     * Gets all the pending JavaScript invocations and clears the queue.
     *
     * @return a list of pending JavaScript invocations
     */
    public List<JavaScriptInvocation> dumpPendingJavaScriptInvocations() {
        pendingTitleUpdateCanceler = null;

        if (pendingJsInvocations.isEmpty()) {
            return Collections.emptyList();
        }

        List<JavaScriptInvocation> currentList = pendingJsInvocations;

        pendingJsInvocations = new ArrayList<>();

        return currentList;
    }

    /**
     * Gets the pending javascript invocations added with
     * {@link #addJavaScriptInvocation(JavaScriptInvocation)} after last
     * {@link #dumpPendingJavaScriptInvocations()}.
     *
     * @return the pending javascript invocations, never <code>null</code>
     */
    // Non-private for testing purposes
    List<JavaScriptInvocation> getPendingJavaScriptInvocations() {
        return pendingJsInvocations;
    }

    /**
     * Records the page title set with {@link Page#setTitle(String)}.
     * <p>
     * You should not set the page title for the browser with this method, use
     * {@link Page#setTitle(String)} instead.
     *
     * @param title
     *            the title to set
     */
    public void setTitle(String title) {
        assert title != null;

        JavaScriptInvocation invocation = new JavaScriptInvocation(
                "document.title = $0", Arrays.asList(title));

        pendingTitleUpdateCanceler = addJavaScriptInvocation(invocation);

        this.title = title;
    }

    /**
     * Gets the page title recorded with {@link Page#setTitle(String)}.
     * <p>
     * <b>NOTE</b> this might not be up to date with the actual title set since
     * it is not updated from the browser and the update might have been
     * canceled before it has been sent to the browser with
     * {@link #cancelPendingTitleUpdate()}.
     *
     * @return the page title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Cancels any pending page title update set via {@link #setTitle(String)}.
     *
     * @return <code>true</code> if pending title update was cancelled,
     *         <code>false</code> if not
     */
    public boolean cancelPendingTitleUpdate() {
        if (pendingTitleUpdateCanceler == null) {
            return false;
        }

        boolean result = pendingTitleUpdateCanceler.cancelExecution();
        pendingTitleUpdateCanceler = null;
        return result;
    }

    /**
     * Shows a view in a chain of layouts in this UI. This method is intended
     * for framework use only. Use {@link UI#navigateTo(String)} to change the
     * view that is shown in a UI.
     *
     * @param viewLocation
     *            the location of the view relative to the servlet serving the
     *            UI, not <code>null</code>
     * @param view
     *            the view to show, not <code>null</code>
     * @param parentViews
     *            the list of parent views to wrap the view in, starting from
     *            the parent view immediately wrapping the main view, or
     *            <code>null</code> to not use any parent views
     */
    public void showView(Location viewLocation, View view,
            List<HasChildView> parentViews) {
        assert view != null;
        assert viewLocation != null;

        this.viewLocation = viewLocation;

        Element uiElement = ui.getElement();

        // Assemble previous parent-child relationships to enable detecting
        // changes
        Map<HasChildView, View> oldChildren = new HashMap<>();
        for (int i = 0; i < viewChain.size() - 1; i++) {
            View child = viewChain.get(i);
            HasChildView parent = (HasChildView) viewChain.get(i + 1);

            oldChildren.put(parent, child);
        }

        viewChain = new ArrayList<>();
        viewChain.add(view);

        if (parentViews != null) {
            viewChain.addAll(parentViews);
        }

        if (viewChain.isEmpty()) {
            uiElement.removeAllChildren();
        } else {
            // Ensure the entire chain is connected
            View root = null;
            for (View part : viewChain) {
                if (root != null) {
                    assert part instanceof HasChildView : "All parts of the chain except the first must implement "
                            + HasChildView.class.getSimpleName();
                    HasChildView parent = (HasChildView) part;
                    if (oldChildren.get(parent) != root) {
                        parent.setChildView(root);
                    }
                } else if (part instanceof HasChildView
                        && oldChildren.containsKey(part)) {
                    // Remove old child view from leaf view if it had one
                    ((HasChildView) part).setChildView(null);
                }
                root = part;
            }

            if (root == null) {
                throw new IllegalArgumentException(
                        "Root can't be null here since we know there's at least one item in the chain");
            }

            Element rootElement = root.getElement();

            if (!uiElement.equals(rootElement.getParent())) {
                uiElement.removeAllChildren();
                rootElement.removeFromParent();
                uiElement.appendChild(rootElement);
            }
        }
    }

    /**
     * Gets the currently active view and parent views.
     *
     * @return a list of view and parent view instances, starting from the
     *         innermost part
     */
    public List<View> getActiveViewChain() {
        return Collections.unmodifiableList(viewChain);
    }

    /**
     * Gets the location of the currently shown view. The location is relative
     * the the servlet mapping used for serving this UI.
     *
     * @return the view location, not <code>null</code>
     */
    public Location getActiveViewLocation() {
        return viewLocation;
    }
}
