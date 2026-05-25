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

import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.HeartbeatCallbacks;
import com.vaadin.client.communication.LoadingIndicatorStateHandler;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.Poller;
import com.vaadin.client.communication.PushConfiguration;
import com.vaadin.client.communication.ReconnectConfiguration;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.XhrConnection;
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
        set(ResourceLoader.class, new ResourceLoader(this, true));
        set("URIResolver", new URIResolver(this));
        set(DependencyLoader.class, new DependencyLoader(this));
        set("UILifecycle", (Supplier<UILifecycle>) UILifecycle::new);
        UILifecycle uiLifecycle = get("UILifecycle");
        StateTree stateTree = new StateTree(this);
        set("StateTree", stateTree);
        RequestResponseTracker requestResponseTracker = new RequestResponseTracker(
                () -> {
                    com.vaadin.client.communication.MessageSender ms = getMessageSender();
                    if ((getUILifecycle().isRunning()
                            && getServerRpcQueue().isFlushPending())
                            || ms.getResynchronizationState() == com.vaadin.client.communication.MessageSender.ResynchronizationState.SEND_TO_SERVER
                            || ms.hasQueuedMessages()) {
                        ms.sendInvocationsToServer();
                    }
                });
        set("RequestResponseTracker", requestResponseTracker);
        set(MessageHandler.class, new MessageHandler(this));
        MessageSender messageSender = new MessageSender(this);
        set(MessageSender.class, messageSender);
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
                            new RuntimeException(message));
                }
            };
            return new Heartbeat(uri,
                    applicationConfiguration.getHeartbeatInterval(),
                    uiLifecycle, callbacks);
        };
        set(Heartbeat.class, heartbeatSupplier);
        set(ConnectionStateHandler.class,
                new DefaultConnectionStateHandler(this));
        set(XhrConnection.class, new XhrConnection(this));
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

}
