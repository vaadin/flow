package com.vaadin.client;

import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.PushConfiguration;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.XhrConnection;
import com.vaadin.client.hummingbird.StateTree;

public class DefaultRegistry extends Registry {

    public DefaultRegistry(ApplicationConnection connection) {
        // Note that initialization order matters. Many constructors depend on
        // ApplicationConnection, ApplicationConfiguration and StateTree even
        // though this is not explicitly specified anywhere.

        set(ApplicationConnection.class, connection);

        // Classes with no constructor dependencies
        set(UILifecycle.class, new UILifecycle());
        set(LoadingIndicator.class, new LoadingIndicator());
        set(RequestResponseTracker.class, new RequestResponseTracker(this));
        set(DependencyLoader.class, new DependencyLoader(this));
        set(URIResolver.class, new URIResolver(this));
        set(StateTree.class, new StateTree(this));
        set(MessageHandler.class, new MessageHandler(this));
        set(MessageSender.class, new MessageSender(this));
        set(ServerRpcQueue.class, new ServerRpcQueue(this));

        // Classes with dependencies, in correct order
        set(Heartbeat.class, new Heartbeat(this));
        set(ConnectionStateHandler.class,
                new DefaultConnectionStateHandler(this));
        set(XhrConnection.class, new XhrConnection(this));
        set(PushConfiguration.class, new PushConfiguration(this));
    }
}
