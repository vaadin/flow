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

package com.vaadin.client.communication;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.Command;
import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.ResourceLoader;
import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.ValueMap;
import com.vaadin.client.WidgetUtil;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.communication.PushConstants;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.JsonObject;

/**
 * The default {@link PushConnection} implementation that uses Atmosphere for
 * handling the communication channel.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class AtmospherePushConnection implements PushConnection {

    /**
     * Represents the connection state of a push connection.
     */
    protected enum State {
        /**
         * Opening request has been sent, but still waiting for confirmation.
         */
        CONNECT_PENDING,

        /**
         * Connection is open and ready to use.
         */
        CONNECTED,

        /**
         * Connection was disconnected while the connection was pending. Wait
         * for the connection to get established before closing it. No new
         * messages are accepted, but pending messages will still be delivered.
         */
        DISCONNECT_PENDING,

        /**
         * Connection has been disconnected and should not be used any more.
         */
        DISCONNECTED;
    }

    /**
     * Represents a message splitted into multiple fragments of maximum length
     * {@link #FRAGMENT_LENGTH}.
     */
    protected static class FragmentedMessage {

        private static final int FRAGMENT_LENGTH = PushConstants.WEBSOCKET_FRAGMENT_SIZE;

        private String message;
        private int index = 0;

        /**
         * Creates a new instance based on the given message.
         *
         * @param message
         *            the message to wrap
         */
        public FragmentedMessage(String message) {
            this.message = message;
        }

        /**
         * Checks if there is another fragment which can be retrieved using
         * {@link #getNextFragment()} or if all fragments have been retrieved.
         *
         * @return true if there is another fragment to retrieve, false
         *         otherwise
         */
        public boolean hasNextFragment() {
            return index < message.length();
        }

        /**
         * Gets the following fragment and increments the internal fragment
         * counter so the following call to this method will return the
         * following fragment.
         * <p>
         * This method should not be called if all fragments have been received
         * ({@link #hasNextFragment()} returns false).
         *
         * @return the next fragment
         */
        public String getNextFragment() {
            assert hasNextFragment();

            String result;
            if (index == 0) {
                String header = "" + message.length()
                        + PushConstants.MESSAGE_DELIMITER;
                int fragmentLen = FRAGMENT_LENGTH - header.length();
                result = header + getFragment(0, fragmentLen);
                index += fragmentLen;
            } else {
                result = getFragment(index, index + FRAGMENT_LENGTH);
                index += FRAGMENT_LENGTH;
            }
            return result;
        }

        private String getFragment(int begin, int end) {
            return message.substring(begin, Math.min(message.length(), end));
        }
    }

    private JavaScriptObject socket;

    private State state = State.CONNECT_PENDING;

    private AtmosphereConfiguration config;

    private String uri;

    private String transport;

    /**
     * Keeps track of the disconnect confirmation command for cases where
     * pending messages should be pushed before actually disconnecting.
     */
    private Command pendingDisconnectCommand;

    /**
     * The url to use for push requests.
     */
    private String url;

    private final Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public AtmospherePushConnection(Registry registry) {
        this.registry = registry;
        registry.getUILifecycle().addHandler(event -> {
            if (event.getUiLifecycle().isTerminated()) {
                if (state == State.DISCONNECT_PENDING
                        || state == State.DISCONNECTED) {
                    return;
                }

                disconnect(() -> {
                });
            }
        });
        config = createConfig();
        // Always debug for now
        config.setStringValue("logLevel", "debug");

        getPushConfiguration().getParameters().forEach((value, key) -> {
            if (value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("false")) {
                config.setBooleanValue(key, value.equalsIgnoreCase("true"));
            } else {
                config.setStringValue(key, value);
            }

        });
        if (getPushConfiguration().getPushUrl() == null) {
            url = registry.getApplicationConfiguration().getServiceUrl();
        } else {
            url = getPushConfiguration().getPushUrl();
        }
        runWhenAtmosphereLoaded(
                () -> Scheduler.get().scheduleDeferred(this::connect));
    }

    private PushConfiguration getPushConfiguration() {
        return registry.getPushConfiguration();
    }

    private ConnectionStateHandler getConnectionStateHandler() {
        return registry.getConnectionStateHandler();
    }

    private void connect() {
        String pushUrl = registry.getURIResolver().resolveVaadinUri(url);
        pushUrl = SharedUtil.addGetParameter(pushUrl,
                ApplicationConstants.REQUEST_TYPE_PARAMETER,
                ApplicationConstants.REQUEST_TYPE_PUSH);
        pushUrl = SharedUtil.addGetParameter(pushUrl,
                ApplicationConstants.UI_ID_PARAMETER,
                registry.getApplicationConfiguration().getUIId());

        String pushId = registry.getMessageHandler().getPushId();
        if (pushId != null) {
            pushUrl = SharedUtil.addGetParameter(pushUrl,
                    ApplicationConstants.PUSH_ID_PARAMETER, pushId);
        }

        Console.log("Establishing push connection");
        socket = doConnect(pushUrl, getConfig());
    }

    @Override
    public boolean isActive() {
        switch (state) {
        case CONNECT_PENDING:
        case CONNECTED:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isBidirectional() {
        if (transport == null) {
            return false;
        }

        if (!transport.equals("websocket")) {
            // If we are not using websockets, we want to send XHRs
            return false;
        }
        if (registry.getPushConfiguration().isAlwaysXhrToServer()) {
            // If user has forced us to use XHR, let's abide
            return false;
        }
        if (state == State.CONNECT_PENDING) {
            // Not sure yet, let's go for using websockets still as still will
            // delay the message until a connection is established. When the
            // connection is established, bi-directionality will be checked
            // again to be sure
        }
        return true;

    };

    @Override
    public void push(JsonObject message) {
        if (!isBidirectional()) {
            throw new IllegalStateException(
                    "This server to client push connection should not be used to send client to server messages");
        }
        if (state == State.CONNECTED) {
            String messageJson = WidgetUtil.stringify(message);
            Console.log("Sending push (" + transport + ") message to server: "
                    + messageJson);

            if (transport.equals("websocket")) {
                FragmentedMessage fragmented = new FragmentedMessage(
                        messageJson);
                while (fragmented.hasNextFragment()) {
                    doPush(socket, fragmented.getNextFragment());
                }
            } else {
                doPush(socket, messageJson);
            }
            return;
        }

        if (state == State.CONNECT_PENDING) {
            getConnectionStateHandler().pushNotConnected(message);
            return;
        }

        throw new IllegalStateException("Can not push after disconnecting");
    }

    protected AtmosphereConfiguration getConfig() {
        return config;
    }

    protected void onReopen(AtmosphereResponse response) {
        Console.log("Push connection re-established using "
                + response.getTransport());
        onConnect(response);
    }

    protected void onOpen(AtmosphereResponse response) {
        Console.log(
                "Push connection established using " + response.getTransport());
        onConnect(response);
    }

    /**
     * Called whenever a server push connection is established (or
     * re-established).
     *
     * @param response
     *            the response
     *
     */
    protected void onConnect(AtmosphereResponse response) {
        transport = response.getTransport();
        switch (state) {
        case CONNECT_PENDING:
            state = State.CONNECTED;
            getConnectionStateHandler().pushOk(this);
            break;
        case DISCONNECT_PENDING:
            // Set state to connected to make disconnect close the connection
            state = State.CONNECTED;
            assert pendingDisconnectCommand != null;
            disconnect(pendingDisconnectCommand);
            break;
        case CONNECTED:
            // IE likes to open the same connection multiple times, just ignore
            break;
        default:
            throw new IllegalStateException(
                    "Got onOpen event when connection state is " + state
                            + ". This should never happen.");
        }
    }

    @Override
    public final void disconnect(Command command) {
        assert command != null;

        switch (state) {
        case CONNECT_PENDING:
            // Make the connection callback initiate the disconnection again
            state = State.DISCONNECT_PENDING;
            pendingDisconnectCommand = command;
            break;
        case CONNECTED:
            // Normal disconnect
            Console.log("Closing push connection");
            doDisconnect(uri);
            state = State.DISCONNECTED;
            command.execute();
            break;
        case DISCONNECT_PENDING:
        case DISCONNECTED:
            throw new IllegalStateException(
                    "Can not disconnect more than once");
        }
    }

    /**
     * Called whenever a message is received by Atmosphere.
     *
     * @param response
     *            the Atmosphere response object, which contains the message
     */
    protected void onMessage(AtmosphereResponse response) {
        String message = response.getResponseBody();
        ValueMap json = MessageHandler.parseWrappedJson(message);
        if (json == null) {
            // Invalid string (not wrapped as expected)
            getConnectionStateHandler().pushInvalidContent(this, message);
            return;
        } else {
            Console.log("Received push (" + getTransportType() + ") message: "
                    + message);
            registry.getMessageHandler().handleMessage(json);
        }
    }

    /**
     * Called if the transport mechanism cannot be used and the fallback will be
     * tried.
     */
    protected void onTransportFailure() {
        Console.warn("Push connection using primary method ("
                + getConfig().getTransport() + ") failed. Trying with "
                + getConfig().getFallbackTransport());
    }

    /**
     * Called if the push connection fails.
     * <p>
     * Atmosphere will automatically retry the connection until successful.
     *
     * @param response
     *            the Atmosphere response for the failed connection
     */
    protected void onError(AtmosphereResponse response) {
        state = State.DISCONNECTED;
        getConnectionStateHandler().pushError(this, response);
    }

    /**
     * Called when the push connection has been closed.
     * <p>
     * This does not necessarily indicate an error and Atmosphere might try to
     * reconnect or downgrade to the fallback transport automatically.
     *
     * @param response
     *            the Atmosphere response which was closed
     */
    protected void onClose(AtmosphereResponse response) {
        state = State.CONNECT_PENDING;
        getConnectionStateHandler().pushClosed(this, response);
    }

    /**
     * Called when the Atmosphere client side timeout occurs.
     * <p>
     * The connection will be closed at this point and reconnect will not happen
     * automatically.
     *
     * @param response
     *            the Atmosphere response which was used when the timeout
     *            occurred
     */
    protected void onClientTimeout(AtmosphereResponse response) {
        state = State.DISCONNECTED;
        getConnectionStateHandler().pushClientTimeout(this, response);
    }

    /**
     * Called when the push connection has lost the connection to the server and
     * will proceed to try to re-establish the connection.
     *
     * @param request
     *            the Atmosphere request
     * @param response
     *            the Atmosphere response
     */
    protected void onReconnect(JavaScriptObject request,
            final AtmosphereResponse response) {
        if (state == State.CONNECTED) {
            state = State.CONNECT_PENDING;
        }
        getConnectionStateHandler().pushReconnectPending(this);
    }

    /**
     * JavaScriptObject class with some helper methods to set and get primitive
     * values.
     */
    public abstract static class AbstractJSO extends JavaScriptObject {
        /**
         * JavaScriptObject constructor.
         */
        protected AbstractJSO() {

        }

        /**
         * Gets the given property value as a String.
         *
         * @param key
         *            the key of the property
         * @return the property value
         */
        protected final native String getStringValue(String key)
        /*-{
           return this[key];
         }-*/;

        /**
         * Sets the given property value as a String.
         *
         * @param key
         *            the key of the property
         * @param value
         *            the property value
         */
        protected final native void setStringValue(String key, String value)
        /*-{
            this[key] = value;
        }-*/;

        /**
         * Gets the given property value as an int.
         *
         * @param key
         *            the key of the property
         * @return the property value
         */
        protected final native int getIntValue(String key)
        /*-{
           return this[key];
         }-*/;

        /**
         * Sets the given property value as an int.
         *
         * @param key
         *            the key of the property
         * @param value
         *            the property value
         */
        protected final native void setIntValue(String key, int value)
        /*-{
            this[key] = value;
        }-*/;

        /**
         * Gets the given property value as a boolean.
         *
         * @param key
         *            the key of the property
         * @return the property value
         */
        protected final native boolean getBooleanValue(String key)
        /*-{
           return this[key];
         }-*/;

        /**
         * Sets the given property value as a boolean.
         *
         * @param key
         *            the key of the property
         * @param value
         *            the property value
         */
        protected final native void setBooleanValue(String key, boolean value)
        /*-{
            this[key] = value;
        }-*/;

    }

    /**
     * Provides information from the Atmosphere configuration object.
     */
    public static class AtmosphereConfiguration extends AbstractJSO {

        /**
         * JavaScriptObject constructor.
         */
        protected AtmosphereConfiguration() {
            super();
        }

        /**
         * Gets the transport mechanism.
         *
         * @return the transport mechanism
         */
        public final String getTransport() {
            return getStringValue("transport");
        }

        /**
         * Gets the fallback transport mechanism.
         *
         * @return the fallback transport mechanism
         */
        public final String getFallbackTransport() {
            return getStringValue("fallbackTransport");
        }

        /**
         * Sets the transport mechanism to use.
         *
         * @param transport
         *            the transport mechanism
         */
        public final void setTransport(String transport) {
            setStringValue("transport", transport);
        }

        /**
         * Sets the fallback transport mechanism to use.
         *
         * @param fallbackTransport
         *            the fallback transport mechanism
         */
        public final void setFallbackTransport(String fallbackTransport) {
            setStringValue("fallbackTransport", fallbackTransport);
        }
    }

    /**
     * Provides data from an Atmosphere response JavaScript object.
     */
    public static class AtmosphereResponse extends AbstractJSO {

        /**
         * JavaScriptObject constructor.
         */
        protected AtmosphereResponse() {

        }

        /**
         * Gets the response status code.
         *
         * @return the response status code.
         */
        public final int getStatusCode() {
            return getIntValue("status");
        }

        /**
         * Gets the response text.
         *
         * @return the response body.
         */
        public final String getResponseBody() {
            return getStringValue("responseBody");
        }

        /**
         * Gets the Atmosphere reported state.
         * <p>
         * The state can be at least {@literal messageReceived},
         * {@literal error}, {@literal opening}, {@literal messagePublished},
         * {@literal re-connecting}, {@literal closedByClient},
         * {@literal re-opening}, {@literal fail-to-reconnect},
         * {@literal unsubscribe}, {@literal closed}
         *
         * @return the state reported by Atmosphere
         */
        public final String getState() {
            return getStringValue("state");
        }

        /**
         * Gets the transport reported by Atmosphere.
         *
         * @return the transport
         */
        public final String getTransport() {
            return getStringValue("transport");
        }

    }

    /**
     * Creates the default Atmosphere configuration object.
     *
     * @return the Atmosphere configuration object
     */
    protected final native AtmosphereConfiguration createConfig()
    /*-{
        return {
            transport: 'websocket',
            maxStreamingLength: 1000000,
            fallbackTransport: 'long-polling',
            contentType: 'application/json; charset=UTF-8',
            reconnectInterval: 5000,
            timeout: -1,
            maxReconnectOnClose: 10000000,
            trackMessageLength: true,
            enableProtocol: true,
            handleOnlineOffline: false,
            messageDelimiter: String.fromCharCode(@com.vaadin.flow.shared.communication.PushConstants::MESSAGE_DELIMITER)
        };
    }-*/;

    private final native JavaScriptObject doConnect(String uri,
            JavaScriptObject config)
    /*-{
        var self = this;

        config.url = uri;
        config.onOpen = $entry(function(response) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onOpen(*)(response);
        });
        config.onReopen = $entry(function(response) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onReopen(*)(response);
        });
        config.onMessage = $entry(function(response) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onMessage(*)(response);
        });
        config.onError = $entry(function(response) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onError(*)(response);
        });
        config.onTransportFailure = $entry(function(reason,request) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onTransportFailure(*)(reason);
        });
        config.onClose = $entry(function(response) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onClose(*)(response);
        });
        config.onReconnect = $entry(function(request, response) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onReconnect(*)(request, response);
        });
        config.onClientTimeout = $entry(function(request) {
            self.@com.vaadin.client.communication.AtmospherePushConnection::onClientTimeout(*)(request);
        });

        return $wnd.vaadinPush.atmosphere.subscribe(config);
    }-*/;

    private native void doPush(JavaScriptObject socket, String message)
    /*-{
       socket.push(message);
    }-*/;

    private static native void doDisconnect(String url)
    /*-{
       $wnd.vaadinPush.atmosphere.unsubscribeUrl(url);
    }-*/;

    private static native boolean isAtmosphereLoaded()
    /*-{
        return $wnd.vaadinPush && $wnd.vaadinPush.atmosphere;
    }-*/;

    private void runWhenAtmosphereLoaded(final Command command) {
        if (isAtmosphereLoaded()) {
            command.execute();
        } else {
            final String pushJs = getVersionedPushJs();

            Console.log("Loading " + pushJs);
            ResourceLoader loader = registry.getResourceLoader();
            String pushScriptUrl = registry.getApplicationConfiguration()
                    .getContextRootUrl() + pushJs;
            ResourceLoadListener loadListener = new ResourceLoadListener() {
                @Override
                public void onLoad(ResourceLoadEvent event) {
                    if (isAtmosphereLoaded()) {
                        Console.log(pushJs + " loaded");
                        command.execute();
                    } else {
                        // If bootstrap tried to load
                        // vaadinPush.js, ResourceLoader assumes it succeeded
                        // even if it failed (#11673)
                        onError(event);
                    }
                }

                @Override
                public void onError(ResourceLoadEvent event) {
                    getConnectionStateHandler()
                            .pushScriptLoadError(event.getResourceData());

                }
            };
            loader.loadScript(pushScriptUrl, loadListener);
        }
    }

    private String getVersionedPushJs() {
        String pushJs;
        if (registry.getApplicationConfiguration().isProductionMode()) {
            pushJs = ApplicationConstants.VAADIN_PUSH_JS;
        } else {
            pushJs = ApplicationConstants.VAADIN_PUSH_DEBUG_JS;
        }
        return pushJs;
    }

    @Override
    public String getTransportType() {
        return transport;
    }

    /**
     * The default {@link PushConnectionFactory} implementation that provides
     * {@link AtmospherePushConnection} instances.
     */
    static class Factory implements PushConnectionFactory {

        @Override
        public PushConnection create(Registry registry) {
            return new AtmospherePushConnection(registry);
        }
    }
}
