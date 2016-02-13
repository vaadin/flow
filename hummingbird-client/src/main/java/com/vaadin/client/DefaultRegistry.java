package com.vaadin.client;

import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.DefaultConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.PushConfiguration;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.XhrConnection;
import com.vaadin.client.hummingbird.StateTree;

public class DefaultRegistry extends Registry {

    public DefaultRegistry(ApplicationConnection connection) {
        // Note that initialization order matters. Many constructors depend on
        // ApplicationConnection, ApplicationConfiguration and StateTree even
        // though this is not explicitly specified anywhere.

        set(ApplicationConnection.class, connection);

        set(StateTree.class, new StateTree(this));
        set(Heartbeat.class, new Heartbeat(this));
        set(LoadingIndicator.class, new LoadingIndicator());

        set(ConnectionStateHandler.class,
                new DefaultConnectionStateHandler(this));

        set(MessageHandler.class, new MessageHandler(this));
        set(MessageSender.class, new MessageSender(this));
        set(ServerRpcQueue.class, new ServerRpcQueue(this));
        set(XhrConnection.class, new XhrConnection(this));

        set(PushConfiguration.class, new PushConfiguration(this));
    }

}
