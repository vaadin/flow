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

import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
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
                this);
        set(RequestResponseTracker.class, requestResponseTracker);
        set(MessageHandler.class, new MessageHandler(this));
        MessageSender messageSender = new MessageSender(this);
        set(MessageSender.class, messageSender);
        set("ServerRpcQueue", new ServerRpcQueue(uiLifecycle,
                () -> messageSender.sendInvocationsToServer()));
        set(ServerConnector.class, new ServerConnector(this));
        set(ExecuteJavaScriptProcessor.class,
                new ExecuteJavaScriptProcessor(this));
        set("ConstantPool", (Supplier<ConstantPool>) ConstantPool::new);
        set("ExistingElementMap",
                (Supplier<ExistingElementMap>) ExistingElementMap::new);
        set("InitialPropertiesHandler",
                new InitialPropertiesHandler(stateTree));

        // Classes with dependencies, in correct order
        Supplier<Heartbeat> heartbeatSupplier = () -> new Heartbeat(this);
        set(Heartbeat.class, heartbeatSupplier);
        set(ConnectionStateHandler.class,
                new DefaultConnectionStateHandler(this));
        set(XhrConnection.class, new XhrConnection(this));
        set(PushConfiguration.class, new PushConfiguration(this));
        set("ReconnectConfiguration", new ReconnectConfiguration(stateTree));
        set("Poller", new Poller(stateTree, uiLifecycle));
        set("LoadingIndicatorStateHandler", new LoadingIndicatorStateHandler(
                requestResponseTracker::hasActiveRequest));
    }

}
