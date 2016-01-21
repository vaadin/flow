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
package com.vaadin.server;

import java.lang.reflect.Method;
import java.util.List;

import com.vaadin.shared.Connector;
import com.vaadin.shared.communication.MethodInvocation;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.ComponentEventListener;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

/**
 * Interface implemented by all connectors that are capable of communicating
 * with the client side
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 *
 */
public interface ClientConnector extends Connector {

    /**
     * Event fired after a connector is attached to the application.
     */
    public static class AttachEvent extends ComponentEvent {
        public static final String ATTACH_EVENT_IDENTIFIER = "clientConnectorAttach";

        public AttachEvent(Component source) {
            super(source);
        }
    }

    /**
     * Interface for listening {@link DetachEvent connector detach events}.
     *
     */
    public static interface AttachListener extends ComponentEventListener {
        public static final Method attachMethod = ReflectTools
                .findMethod(AttachListener.class, "attach", AttachEvent.class);

        /**
         * Called when a AttachListener is notified of a AttachEvent.
         *
         * @param event
         *            The attach event that was fired.
         */
        public void attach(AttachEvent event);
    }

    /**
     * Event fired before a connector is detached from the application.
     */
    public static class DetachEvent extends ComponentEvent {
        public static final String DETACH_EVENT_IDENTIFIER = "clientConnectorDetach";

        public DetachEvent(Component source) {
            super(source);
        }
    }

    /**
     * Interface for listening {@link DetachEvent connector detach events}.
     *
     */
    public static interface DetachListener extends ComponentEventListener {
        public static final Method detachMethod = ReflectTools
                .findMethod(DetachListener.class, "detach", DetachEvent.class);

        /**
         * Called when a DetachListener is notified of a DetachEvent.
         *
         * @param event
         *            The detach event that was fired.
         */
        public void detach(DetachEvent event);
    }

    public void addAttachListener(AttachListener listener);

    public void removeAttachListener(AttachListener listener);

    public void addDetachListener(DetachListener listener);

    public void removeDetachListener(DetachListener listener);

    /**
     * An error event for connector related errors. Use {@link #getConnector()}
     * to find the connector where the error occurred or {@link #getComponent()}
     * to find the nearest parent component.
     */
    public static class ConnectorErrorEvent
            extends com.vaadin.server.ErrorEvent {

        private Connector connector;

        public ConnectorErrorEvent(Connector connector, Throwable t) {
            super(t);
            this.connector = connector;
        }

        /**
         * Gets the connector for which this error occurred.
         *
         * @return The connector for which the error occurred
         */
        public Connector getConnector() {
            return connector;
        }

    }

    /**
     * Returns the list of pending server to client RPC calls and clears the
     * list.
     *
     * @return an unmodifiable ordered list of pending server to client method
     *         calls (not null)
     */
    public List<ClientMethodInvocation> retrievePendingRpcCalls();

    /**
     * Checks if the communicator is enabled. An enabled communicator is allowed
     * to receive messages from its counter-part.
     *
     * @return true if the connector can receive messages, false otherwise
     */
    public boolean isConnectorEnabled();

    @Override
    public ClientConnector getParent();

    /**
     * Marks that this connector's state might have changed. When the framework
     * is about to send new data to the client-side, it will run
     * {@link #beforeClientResponse(boolean)} followed by {@link #encodeState()}
     * for all connectors that are marked as dirty and send any updated state
     * info to the client.
     *
     * @since 7.0.0
     */
    public void markAsDirty();

    /**
     * Causes this connector and all connectors below it to be marked as dirty.
     * <p>
     * This should only be used in special cases, e.g when the state of a
     * descendant depends on the state of an ancestor.
     *
     * @see #markAsDirty()
     *
     * @since 7.0.0
     */
    public void markAsDirtyRecursive();

    /**
     * Checks if the connector is attached to a VaadinSession.
     *
     * @since 7.1
     * @return true if the connector is attached to a session, false otherwise
     */
    public boolean isAttached();

    /**
     * Notifies the connector that it is connected to a VaadinSession (and
     * therefore also to a UI).
     * <p>
     * The caller of this method is {@link #setParent(ClientConnector)} if the
     * parent is itself already attached to the session. If not, the parent will
     * call the {@link #attach()} for all its children when it is attached to
     * the session. This method is always called before the connector's data is
     * sent to the client-side for the first time.
     * </p>
     *
     * <p>
     * The attachment logic is implemented in {@link AbstractClientConnector}.
     * </p>
     */
    public void attach();

    /**
     * Notifies the connector that it is detached from its VaadinSession.
     *
     * <p>
     * The caller of this method is {@link #setParent(ClientConnector)} if the
     * parent is in the session. When the parent is detached from the session it
     * is its responsibility to call {@link #detach()} for each of its children.
     *
     * </p>
     */
    public void detach();

    /**
     * Returns the UI this connector is attached to
     *
     * @return The UI this connector is attached to or null if it is not
     *         attached to any UI
     */
    public UI getUI();

    /**
     * Called before the shared state and RPC invocations are sent to the
     * client. Gives the connector an opportunity to set computed/dynamic state
     * values or to invoke last minute RPC methods depending on other component
     * features.
     *
     * @param initial
     *            <code>true</code> if the client-side connector will be created
     *            and initialized after this method has been invoked.
     *            <code>false</code> if there is already an initialized
     *            client-side connector.
     *
     * @since 7.0
     */
    public void beforeClientResponse(boolean initial);

    /**
     * Gets the error handler for the connector.
     *
     * The error handler is dispatched whenever there is an error processing the
     * data coming from the client to this connector.
     *
     * @return The error handler or null if not set
     */
    public ErrorHandler getErrorHandler();

    /**
     * Sets the error handler for the connector.
     *
     * The error handler is dispatched whenever there is an error processing the
     * data coming from the client for this connector.
     *
     * @param errorHandler
     *            The error handler for this connector
     */
    public void setErrorHandler(ErrorHandler errorHandler);

    /**
     * Temporary way to handle RPC calls
     *
     * @param invocation
     */
    public void handleRpc(MethodInvocation invocation);
}
