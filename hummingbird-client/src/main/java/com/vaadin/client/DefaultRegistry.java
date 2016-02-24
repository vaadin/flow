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
package com.vaadin.client;

import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.PushConfiguration;
import com.vaadin.client.communication.ReconnectDialogConfiguration;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.XhrConnection;
import com.vaadin.client.hummingbird.ExecuteJavaScriptProcessor;
import com.vaadin.client.hummingbird.StateTree;

/**
 * A registry implementation used by {@link ApplicationConnection}.
 *
 * @author Vaadin
 * @since
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
        set(ApplicationConfiguration.class, applicationConfiguration);

        // Classes with no constructor dependencies
        set(URIResolver.class, new URIResolver(this));
        set(DependencyLoader.class, new DependencyLoader(this));
        set(SystemErrorHandler.class, new SystemErrorHandler(this));
        set(UILifecycle.class, new UILifecycle());
        set(StateTree.class, new StateTree(this));
        set(LoadingIndicator.class, new LoadingIndicator());
        set(RequestResponseTracker.class, new RequestResponseTracker(this));
        set(MessageHandler.class, new MessageHandler(this));
        set(MessageSender.class, new MessageSender(this));
        set(ServerRpcQueue.class, new ServerRpcQueue(this));
        set(ExecuteJavaScriptProcessor.class,
                new ExecuteJavaScriptProcessor(this));

        // Classes with dependencies, in correct order
        set(Heartbeat.class, new Heartbeat(this));
        set(ConnectionStateHandler.class,
                new DefaultConnectionStateHandler(this));
        set(XhrConnection.class, new XhrConnection(this));
        set(PushConfiguration.class, new PushConfiguration(this));
        set(ReconnectDialogConfiguration.class,
                new ReconnectDialogConfiguration(this));
    }
}
