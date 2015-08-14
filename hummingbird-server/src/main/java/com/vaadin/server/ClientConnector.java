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

import java.io.IOException;
import java.util.List;

import com.vaadin.shared.Connector;
import com.vaadin.shared.communication.SharedState;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

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

    /**
     * Returns the type of the shared state for this connector
     *
     * @return The type of the state. Must never return null.
     */
    public Class<? extends SharedState> getStateType();

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
     * Called by the framework to encode the state to a JSONObject. This is
     * typically done by calling the static method
     * {@link LegacyCommunicationManager#encodeState(ClientConnector, SharedState)}
     * .
     *
     * @return a JSON object with the encoded connector state
     */
    public JsonObject encodeState();

    /**
     * Handle a request directed to this connector. This can be used by
     * connectors to dynamically generate a response and it is also used
     * internally when serving {@link ConnectorResource}s.
     * <p>
     * Requests to <code>/APP/connector/[ui id]/[connector id]/</code> are
     * routed to this method with the remaining part of the requested path
     * available in the path parameter.
     * <p>
     * NOTE that the session is not locked when this method is called. It is the
     * responsibility of the connector to ensure that the session is locked
     * while handling state or other session related data. For best performance
     * the session should be unlocked before writing a large response to the
     * client.
     * </p>
     *
     * @param request
     *            the request that should be handled
     * @param response
     *            the response object to which the response should be written
     * @param path
     *            the requested relative path
     * @return <code>true</code> if the request has been handled,
     *         <code>false</code> if no response has been written.
     * @throws IOException
     *             if there is a problem generating a response.
     */
    public boolean handleConnectorRequest(VaadinRequest request,
            VaadinResponse response, String path) throws IOException;

    /**
     * Returns the RPC manager instance to use when receiving calls for an RPC
     * interface.
     *
     * @param rpcInterfaceName
     *            name of the interface for which the call was made
     * @return ServerRpcManager or null if none found for the interface
     */
    public ServerRpcManager<?> getRpcManager(String rpcInterfaceName);

}
