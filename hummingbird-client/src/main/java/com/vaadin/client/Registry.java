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

    public Registry(ApplicationConnection connection,
            ApplicationConfiguration applicationConfiguration) {
        // Note that initialization order matters. Many constructors depend on
        // ApplicationConnection, ApplicationConfiguration and StateTree even
        // though this is not explicitly specified anywhere.

        set(ApplicationConnection.class, connection);
        set(ApplicationConfiguration.class, applicationConfiguration);

        // Classes with no constructor dependencies
        set(UILifecycle.class, new UILifecycle());
        set(LoadingIndicator.class, new LoadingIndicator());
        set(RequestResponseTracker.class, new RequestResponseTracker(this));
        set(SystemErrorHandler.class, new SystemErrorHandler(this));
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
        return get(ApplicationConfiguration.class);
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

    public URIResolver getURIResolver() {
        return get(URIResolver.class);
    }

    public DependencyLoader getDependencyLoader() {
        return get(DependencyLoader.class);
    }

    public RequestResponseTracker getRequestResponseTracker() {
        return get(RequestResponseTracker.class);
    }

    public SystemErrorHandler getSystemErrorHandler() {
        return get(SystemErrorHandler.class);
    }
}
