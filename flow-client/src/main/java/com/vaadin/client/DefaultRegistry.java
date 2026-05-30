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
package com.vaadin.client;

import java.util.function.Supplier;

import com.google.gwt.xhr.client.XMLHttpRequest;

import com.vaadin.client.communication.AtmospherePushConnection;
import com.vaadin.client.communication.AtmospherePushConnectionCallbacks;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandlerCallbacks;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.HeartbeatCallbacks;
import com.vaadin.client.communication.LoadingIndicatorStateHandler;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageHandlerCallbacks;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.MessageSenderCallbacks;
import com.vaadin.client.communication.Poller;
import com.vaadin.client.communication.PushConfiguration;
import com.vaadin.client.communication.ReconnectConfiguration;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.XhrConnection;
import com.vaadin.client.communication.XhrConnectionCallbacks;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.ExecuteJavaScriptProcessor;
import com.vaadin.client.flow.StateTree;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.util.SharedUtil;

/**
 * A registry implementation used by {@link ApplicationConnection}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultRegistry extends Registry {

    /**
     * Constructs a registry based on the given application connection and
     * configuration references.
     *
     * @param connection
     *            the application connection
     * @param applicationConfiguration
     *            the application configuration
     */
    public DefaultRegistry(ApplicationConnection connection,
            ApplicationConfiguration applicationConfiguration) {
        // Note that initialization order matters. Many constructors depend on
        // ApplicationConnection, ApplicationConfiguration and StateTree even
        // though this is not explicitly specified anywhere.

        set(ApplicationConnection.class, connection);
        set("ApplicationConfiguration", applicationConfiguration);

        // Classes with no constructor dependencies
        ResourceLoader resourceLoader = new ResourceLoader(
                message -> getSystemErrorHandler().handleError(message), true);
        set("ResourceLoader", resourceLoader);
        URIResolver uriResolver = new URIResolver(this);
        set("URIResolver", uriResolver);
        set("DependencyLoader",
                new DependencyLoader(uriResolver, resourceLoader));
        set("UILifecycle", (Supplier<UILifecycle>) UILifecycle::new);
        UILifecycle uiLifecycle = get("UILifecycle");
        StateTree stateTree = new StateTree(this);
        set("StateTree", stateTree);
        RequestResponseTracker requestResponseTracker = new RequestResponseTracker(
                () -> {
                    MessageSender ms = getMessageSender();
                    if ((getUILifecycle().isRunning()
                            && getServerRpcQueue().isFlushPending())
                            || ms.getResynchronizationState() == MessageSender.ResynchronizationState.SEND_TO_SERVER
                            || ms.hasQueuedMessages()) {
                        ms.sendInvocationsToServer();
                    }
                });
        set("RequestResponseTracker", requestResponseTracker);

        MessageHandlerCallbacks messageHandlerCallbacks = new MessageHandlerCallbacks();
        messageHandlerCallbacks.setGetMessageSender(this::getMessageSender);
        messageHandlerCallbacks.setGetUiLifecycle(this::getUILifecycle);
        messageHandlerCallbacks.setGetStateTree(this::getStateTree);
        messageHandlerCallbacks.setGetConstantPool(this::getConstantPool);
        messageHandlerCallbacks
                .setGetSystemErrorHandler(this::getSystemErrorHandler);
        messageHandlerCallbacks.setGetExecuteJavaScriptProcessor(
                this::getExecuteJavaScriptProcessor);
        messageHandlerCallbacks
                .setGetDependencyLoader(this::getDependencyLoader);
        messageHandlerCallbacks.setGetResourceLoader(this::getResourceLoader);
        messageHandlerCallbacks
                .setGetRequestResponseTracker(this::getRequestResponseTracker);
        messageHandlerCallbacks.setGetLoadingIndicatorStateHandler(
                this::getLoadingIndicatorStateHandler);
        messageHandlerCallbacks.setGetApplicationConfiguration(
                this::getApplicationConfiguration);
        messageHandlerCallbacks.setRedirect(WidgetUtil::redirect);
        set("MessageHandler", new MessageHandler(messageHandlerCallbacks));

        MessageSenderCallbacks messageSenderCallbacks = new MessageSenderCallbacks();
        messageSenderCallbacks.setGetMessageHandler(this::getMessageHandler);
        messageSenderCallbacks.setGetUiLifecycle(this::getUILifecycle);
        messageSenderCallbacks
                .setGetRequestResponseTracker(this::getRequestResponseTracker);
        messageSenderCallbacks.setGetLoadingIndicatorStateHandler(
                this::getLoadingIndicatorStateHandler);
        messageSenderCallbacks
                .setGetPushConfiguration(this::getPushConfiguration);
        messageSenderCallbacks.setGetServerRpcQueue(this::getServerRpcQueue);
        messageSenderCallbacks.setGetApplicationConfiguration(
                this::getApplicationConfiguration);
        messageSenderCallbacks
                .setSendXhr(payload -> getXhrConnection().send(payload));
        messageSenderCallbacks.setGetXhrUri(() -> getXhrConnection().getUri());
        messageSenderCallbacks
                .setCreatePushConnection(this::createPushConnection);
        MessageSender messageSender = new MessageSender(messageSenderCallbacks);
        set("MessageSender", messageSender);

        ServerRpcQueue serverRpcQueue = new ServerRpcQueue(uiLifecycle,
                () -> messageSender.sendInvocationsToServer());
        set("ServerRpcQueue", serverRpcQueue);
        LoadingIndicatorStateHandler loadingIndicatorStateHandler = new LoadingIndicatorStateHandler(
                requestResponseTracker::hasActiveRequest);
        set("LoadingIndicatorStateHandler", loadingIndicatorStateHandler);
        ServerConnector serverConnector = new ServerConnector(
                loadingIndicatorStateHandler, serverRpcQueue);
        set("ServerConnector", serverConnector);
        stateTree.setServerConnector(serverConnector);
        set("ExecuteJavaScriptProcessor", new ExecuteJavaScriptProcessor(this));
        set("ConstantPool", (Supplier<ConstantPool>) ConstantPool::new);
        set("ExistingElementMap",
                (Supplier<ExistingElementMap>) ExistingElementMap::new);
        InitialPropertiesHandler initialPropertiesHandler = new InitialPropertiesHandler(
                stateTree);
        set("InitialPropertiesHandler", initialPropertiesHandler);
        stateTree.setInitialPropertiesHandler(initialPropertiesHandler);

        // Classes with dependencies, in correct order
        Supplier<Heartbeat> heartbeatSupplier = () -> {
            String uri = SharedUtil.addGetParameter(
                    applicationConfiguration.getServiceUrl(),
                    ApplicationConstants.REQUEST_TYPE_PARAMETER,
                    ApplicationConstants.REQUEST_TYPE_HEARTBEAT);
            uri = SharedUtil.addGetParameter(uri,
                    ApplicationConstants.UI_ID_PARAMETER,
                    applicationConfiguration.getUIId());
            HeartbeatCallbacks callbacks = new HeartbeatCallbacks() {
                @Override
                public void onOk() {
                    getConnectionStateHandler().heartbeatOk();
                }

                @Override
                public void onInvalidStatusCode(XMLHttpRequest xhr) {
                    getConnectionStateHandler().heartbeatInvalidStatusCode(xhr);
                }

                @Override
                public void onException(XMLHttpRequest xhr, String message) {
                    getConnectionStateHandler().heartbeatException(xhr,
                            message);
                }
            };
            return new Heartbeat(uri,
                    applicationConfiguration.getHeartbeatInterval(),
                    uiLifecycle, callbacks);
        };
        set(Heartbeat.class, heartbeatSupplier);

        DefaultConnectionStateHandlerCallbacks connectionStateCallbacks = new DefaultConnectionStateHandlerCallbacks();
        connectionStateCallbacks.setGetUiLifecycle(this::getUILifecycle);
        connectionStateCallbacks
                .setGetSystemErrorHandler(this::getSystemErrorHandler);
        connectionStateCallbacks.setGetHeartbeat(this::getHeartbeat);
        connectionStateCallbacks
                .setGetReconnectConfiguration(this::getReconnectConfiguration);
        connectionStateCallbacks
                .setGetRequestResponseTracker(this::getRequestResponseTracker);
        connectionStateCallbacks.setGetLoadingIndicatorStateHandler(
                this::getLoadingIndicatorStateHandler);
        connectionStateCallbacks.setGetApplicationConfiguration(
                this::getApplicationConfiguration);
        connectionStateCallbacks.setGetMessageSender(this::getMessageSender);
        set("ConnectionStateHandler",
                new DefaultConnectionStateHandler(connectionStateCallbacks));
        XhrConnectionCallbacks xhrConnectionCallbacks = new XhrConnectionCallbacks();
        xhrConnectionCallbacks.setGetMessageHandler(this::getMessageHandler);
        xhrConnectionCallbacks
                .setGetConnectionStateHandler(this::getConnectionStateHandler);
        xhrConnectionCallbacks
                .setGetRequestResponseTracker(this::getRequestResponseTracker);
        xhrConnectionCallbacks.setGetApplicationConfiguration(
                this::getApplicationConfiguration);
        set("XhrConnection", new XhrConnection(xhrConnectionCallbacks));
        set("PushConfiguration",
                new PushConfiguration(stateTree,
                        () -> messageSender.setPushEnabled(true),
                        () -> messageSender.setPushEnabled(false)));
        set("ReconnectConfiguration", new ReconnectConfiguration(stateTree));
        set("Poller", new Poller(stateTree, uiLifecycle));

        // Wire SystemErrorHandler last so its callbacks can reach the rest of
        // the registry through `this::get*` lookups without forward references.
        SystemErrorHandlerCallbacks systemErrorCallbacks = new SystemErrorHandlerCallbacks() {
            @Override
            public String getServiceUrl() {
                return applicationConfiguration.getServiceUrl();
            }

            @Override
            public boolean isWebComponentMode() {
                return applicationConfiguration.isWebComponentMode();
            }

            @Override
            public boolean isProductionMode() {
                return applicationConfiguration.isProductionMode();
            }

            @Override
            public com.vaadin.client.bootstrap.ErrorMessage getSessionExpiredError() {
                return applicationConfiguration.getSessionExpiredError();
            }

            @Override
            public String[] getExportedWebComponents() {
                return applicationConfiguration.getExportedWebComponents();
            }

            @Override
            public int getHeartbeatInterval() {
                return applicationConfiguration.getHeartbeatInterval();
            }

            @Override
            public void setHeartbeatInterval(int seconds) {
                getHeartbeat().setInterval(seconds);
            }

            @Override
            public boolean isPushEnabled() {
                return getPushConfiguration().isPushEnabled();
            }

            @Override
            public void setPushEnabled(boolean enabled) {
                getMessageSender().setPushEnabled(enabled);
            }

            @Override
            public void disablePushImmediately() {
                getMessageSender().setPushEnabled(false, false);
            }

            @Override
            public void applyResyncResponse(String responseText) {
                int uiId = applicationConfiguration.getUIId();
                com.vaadin.client.ValueMap json = MessageHandler
                        .parseJson(responseText);
                int newUiId = json.getInt(ApplicationConstants.UI_ID);
                if (newUiId != uiId) {
                    com.vaadin.client.Console
                            .debug("UI ID switched from " + uiId + " to "
                                    + newUiId + " after resynchronization");
                    applicationConfiguration.setUIId(newUiId);
                }
                reset();
                getUILifecycle().setState(UILifecycle.UIState.RUNNING);
                getMessageHandler().handleMessage(json);
            }
        };
        // Keyed by an explicit string because the Registry uses
        // Class.getName() as a key by default; GWT collapses every native
        // @JsType to JavaScriptObject so multiple such registrations would
        // collide (e.g. with the Heartbeat registration above).
        set("SystemErrorHandler", new SystemErrorHandler(systemErrorCallbacks));
    }

    /**
     * Builds the callbacks struct for a new {@link AtmospherePushConnection}
     * and returns the freshly constructed instance. Used by
     * {@link MessageSender} (through the {@code createPushConnection} callback)
     * so the push connection is only instantiated when push is actually
     * enabled.
     */
    private AtmospherePushConnection createPushConnection() {
        AtmospherePushConnectionCallbacks pushCallbacks = new AtmospherePushConnectionCallbacks();
        pushCallbacks.setGetUiLifecycle(this::getUILifecycle);
        pushCallbacks.setGetPushConfiguration(this::getPushConfiguration);
        pushCallbacks.setGetApplicationConfiguration(
                this::getApplicationConfiguration);
        pushCallbacks.setGetURIResolver(this::getURIResolver);
        pushCallbacks.setGetMessageHandler(this::getMessageHandler);
        pushCallbacks
                .setGetConnectionStateHandler(this::getConnectionStateHandler);
        pushCallbacks.setGetResourceLoader(this::getResourceLoader);
        return new AtmospherePushConnection(pushCallbacks);
    }

}
