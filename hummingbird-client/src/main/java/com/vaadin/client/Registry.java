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
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;

/**
 * A registry of singleton instances, such as {@link ServerRpcQueue}, which can
 * be looked up based on their class.
 *
 * @author Vaadin
 * @since
 */
public class Registry {

    private JsMap<Class<?>, Object> lookupTable = JsCollections.map();

    public Registry(ApplicationConnection connection) {
        // Note that initialization order matters. Many constructors depend on
        // ApplicationConnection, ApplicationConfiguration and StateTree even
        // though this is not explicitly specified anywhere.

        set(ApplicationConnection.class, connection);
        set(UILifecycle.class, new UILifecycle());
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

    /**
     * Stores an instance of the given type.
     * <p>
     * Note that all instances are considered final and you are not allowed to
     * update an instance of any given type.
     *
     * @param type
     *            the type to store
     * @param instance
     *            the instance to store
     */
    public <T, U extends T> void set(Class<T> type, U instance) {
        assert !lookupTable.has(type) : "Registry already has a class of type "
                + type.getName() + " registered";
        lookupTable.set(type, instance);
    }

    /**
     * Gets an instance of the given type from the lookup table.
     *
     * @param type
     *            the type to get
     * @return the stored instance or null if no instance has been stored
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        assert lookupTable.has(type) : "Tried to lookup type " + type.getName()
                + " but no instance has been registered";
        return (T) lookupTable.get(type);
    }

    /**
     * Gets the {@link MessageSender} singleton
     *
     * @return the {@link MessageSender} singleton
     */
    public MessageSender getMessageSender() {
        return get(MessageSender.class);
    }

    public MessageHandler getMessageHandler() {
        return get(MessageHandler.class);
    }

    public LoadingIndicator getLoadingIndicator() {
        return get(LoadingIndicator.class);
    }

    public ApplicationConnection getApplicationConnection() {
        return get(ApplicationConnection.class);
    }

    public Heartbeat getHeartbeat() {
        return get(Heartbeat.class);
    }

    public ConnectionStateHandler getConnectionStateHandler() {
        return get(ConnectionStateHandler.class);
    }

    public ServerRpcQueue getServerRpcQueue() {
        return get(ServerRpcQueue.class);
    }

    public ApplicationConfiguration getApplicationConfiguration() {
        return get(ApplicationConnection.class).getConfiguration();
    }

    public StateTree getStateTree() {
        return get(StateTree.class);
    }

    public PushConfiguration getPushConfiguration() {
        return get(PushConfiguration.class);
    }

    public XhrConnection getXhrConnection() {
        return get(XhrConnection.class);
    }

    public UILifecycle getUILifecycle() {
        return get(UILifecycle.class);
    }

}
