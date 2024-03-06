/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import java.util.function.Supplier;

import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
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

import elemental.events.PopStateEvent;
import elemental.json.JsonObject;

/**
 * A registry implementation used by {@link ApplicationConnection}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultRegistry extends Registry {

    private static class WebComponentScrollHandler
            extends ScrollPositionHandler {

        private WebComponentScrollHandler() {
        }

        @Override
        public void onPopStateEvent(PopStateEvent event,
                boolean triggersServerSideRoundtrip) {
            // don't do anything
        }

        @Override
        public void setIgnoreScrollRestorationOnNextPopStateEvent(
                boolean ignoreScrollRestorationOnNextPopStateEvent) {
            // don't do anything
        }

        @Override
        public void beforeClientNavigation(String newHref) {
            // don't do anything
        }

        @Override
        public void afterServerNavigation(JsonObject state) {
            // don't do anything
        }
    }

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
        set(ResourceLoader.class, new ResourceLoader(this, true));
        set(URIResolver.class, new URIResolver(this));
        set(DependencyLoader.class, new DependencyLoader(this));
        set(SystemErrorHandler.class, new SystemErrorHandler(this));
        set(UILifecycle.class, (Supplier<UILifecycle>) UILifecycle::new);
        set(StateTree.class, new StateTree(this));
        set(RequestResponseTracker.class, new RequestResponseTracker(this));
        set(MessageHandler.class, new MessageHandler(this));
        set(MessageSender.class, new MessageSender(this));
        set(ServerRpcQueue.class, new ServerRpcQueue(this));
        set(ServerConnector.class, new ServerConnector(this));
        set(ExecuteJavaScriptProcessor.class,
                new ExecuteJavaScriptProcessor(this));
        set(ConstantPool.class, (Supplier<ConstantPool>) ConstantPool::new);
        set(ExistingElementMap.class,
                (Supplier<ExistingElementMap>) ExistingElementMap::new);
        set(InitialPropertiesHandler.class, new InitialPropertiesHandler(this));

        // Classes with dependencies, in correct order
        set(Heartbeat.class, new Heartbeat(this));
        set(ConnectionStateHandler.class,
                new DefaultConnectionStateHandler(this));
        set(XhrConnection.class, new XhrConnection(this));
        set(PushConfiguration.class, new PushConfiguration(this));
        set(ReconnectConfiguration.class, new ReconnectConfiguration(this));
        if (!applicationConfiguration.isClientRouting()) {
            if (applicationConfiguration.isWebComponentMode()) {
                set(ScrollPositionHandler.class,
                        new WebComponentScrollHandler());
            } else {
                set(ScrollPositionHandler.class,
                        new ScrollPositionHandler(this));
            }
        }
        set(Poller.class, new Poller(this));
    }

}
