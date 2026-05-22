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
        set(SystemErrorHandler.class, new SystemErrorHandler(this));
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
    }

}
