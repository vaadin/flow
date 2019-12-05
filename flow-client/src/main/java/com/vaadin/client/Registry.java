/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.Poller;
import com.vaadin.client.communication.PushConfiguration;
import com.vaadin.client.communication.ReconnectDialogConfiguration;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.XhrConnection;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.ExecuteJavaScriptProcessor;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;

/**
 * A registry of singleton instances, such as {@link ServerRpcQueue}, which can
 * be looked up based on their class.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Registry {

    private JsMap<Class<?>, Object> lookupTable = JsCollections.map();

    /**
     * Creates a new empty registry.
     * <p>
     * Use {@link #set(Class, Object)} to populate the registry.
     */
    public Registry() {
        // Nothing to do here.
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
     * @param <T>
     *            the type
     */
    protected final <T> void set(Class<T> type, T instance) {
        assert !lookupTable.has(type) : "Registry already has a class of type "
                + type.getName() + " registered";
        lookupTable.set(type, instance);
    }

    /**
     * Gets an instance of the given type from the lookup table.
     *
     * @param type
     *            the type to get
     * @param <T>
     *            the class type
     * @return the stored instance or null if no instance has been stored
     */
    @SuppressWarnings("unchecked")
    protected final <T> T get(Class<T> type) {
        assert lookupTable.has(type) : "Tried to lookup type " + type.getName()
                + " but no instance has been registered";
        return (T) lookupTable.get(type);
    }

    /**
     * Gets the {@link MessageSender} singleton.
     *
     * @return the {@link MessageSender} singleton
     */
    public MessageSender getMessageSender() {
        return get(MessageSender.class);
    }

    /**
     * Gets the {@link MessageHandler} singleton.
     *
     * @return the {@link MessageHandler} singleton
     */
    public MessageHandler getMessageHandler() {
        return get(MessageHandler.class);
    }

    /**
     * Gets the {@link LoadingIndicator} singleton.
     *
     * @return the {@link LoadingIndicator} singleton
     */
    public LoadingIndicator getLoadingIndicator() {
        return get(LoadingIndicator.class);
    }

    /**
     * Gets the {@link ApplicationConnection} singleton.
     *
     * @return the {@link ApplicationConnection} singleton
     */
    public ApplicationConnection getApplicationConnection() {
        return get(ApplicationConnection.class);
    }

    /**
     * Gets the {@link Heartbeat} singleton.
     *
     * @return the {@link Heartbeat} singleton
     */
    public Heartbeat getHeartbeat() {
        return get(Heartbeat.class);
    }

    /**
     * Gets the {@link ConnectionStateHandler} singleton.
     *
     * @return the {@link ConnectionStateHandler} singleton
     */
    public ConnectionStateHandler getConnectionStateHandler() {
        return get(ConnectionStateHandler.class);
    }

    /**
     * Gets the {@link ServerRpcQueue} singleton.
     *
     * @return the {@link ServerRpcQueue} singleton
     */
    public ServerRpcQueue getServerRpcQueue() {
        return get(ServerRpcQueue.class);
    }

    /**
     * Gets the {@link ApplicationConfiguration} singleton.
     *
     * @return the {@link ApplicationConfiguration} singleton
     */
    public ApplicationConfiguration getApplicationConfiguration() {
        return get(ApplicationConfiguration.class);
    }

    /**
     * Gets the {@link StateTree} singleton.
     *
     * @return the {@link StateTree} singleton
     */
    public StateTree getStateTree() {
        return get(StateTree.class);
    }

    /**
     * Gets the {@link PushConfiguration} singleton.
     *
     * @return the {@link PushConfiguration} singleton
     */
    public PushConfiguration getPushConfiguration() {
        return get(PushConfiguration.class);
    }

    /**
     * Gets the {@link XhrConnection} singleton.
     *
     * @return the {@link XhrConnection} singleton
     */
    public XhrConnection getXhrConnection() {
        return get(XhrConnection.class);
    }

    /**
     * Gets the {@link URIResolver} singleton.
     *
     * @return the {@link URIResolver} singleton
     */
    public URIResolver getURIResolver() {
        return get(URIResolver.class);
    }

    /**
     * Gets the {@link DependencyLoader} singleton.
     *
     * @return the {@link DependencyLoader} singleton
     */
    public DependencyLoader getDependencyLoader() {
        return get(DependencyLoader.class);
    }

    /**
     * Gets the {@link SystemErrorHandler} singleton.
     *
     * @return the {@link SystemErrorHandler} singleton
     */
    public SystemErrorHandler getSystemErrorHandler() {
        return get(SystemErrorHandler.class);
    }

    /**
     * Gets the {@link UILifecycle} singleton.
     *
     * @return the {@link UILifecycle} singleton
     */
    public UILifecycle getUILifecycle() {
        return get(UILifecycle.class);
    }

    /**
     * Gets the {@link RequestResponseTracker} singleton.
     *
     * @return the {@link RequestResponseTracker} singleton
     */
    public RequestResponseTracker getRequestResponseTracker() {
        return get(RequestResponseTracker.class);
    }

    /**
     * Gets the {@link ReconnectDialogConfiguration} singleton.
     *
     * @return the {@link ReconnectDialogConfiguration} singleton
     */
    public ReconnectDialogConfiguration getReconnectDialogConfiguration() {
        return get(ReconnectDialogConfiguration.class);
    }

    /**
     * Gets the {@link ExecuteJavaScriptProcessor} singleton.
     *
     * @return the {@link ExecuteJavaScriptProcessor} singleton
     */
    public ExecuteJavaScriptProcessor getExecuteJavaScriptProcessor() {
        return get(ExecuteJavaScriptProcessor.class);
    }

    /**
     * Gets the {@link ServerConnector} singleton.
     *
     * @return the {@link ServerConnector} singleton
     *
     */
    public ServerConnector getServerConnector() {
        return get(ServerConnector.class);
    }

    /**
     * Gets the {@link ResourceLoader} singleton.
     *
     * @return the {@link ResourceLoader} singleton
     */
    public ResourceLoader getResourceLoader() {
        return get(ResourceLoader.class);
    }

    /**
     * Gets the {@link ConstantPool} singleton.
     *
     * @return the {@link ConstantPool} singleton
     */
    public ConstantPool getConstantPool() {
        return get(ConstantPool.class);
    }

    /**
     * Gets the {@link ScrollPositionHandler} singleton.
     *
     * @return the {@link ScrollPositionHandler} singleton
     */
    public ScrollPositionHandler getScrollPositionHandler() {
        return get(ScrollPositionHandler.class);
    }

    /**
     * Gets the {@link ExistingElementMap} singleton.
     *
     * @return the {@link ExistingElementMap} singleton
     */
    public ExistingElementMap getExistingElementMap() {
        return get(ExistingElementMap.class);
    }

    /**
     * Gets the {@link InitialPropertiesHandler} singleton.
     *
     * @return the {@link InitialPropertiesHandler} singleton
     */
    public InitialPropertiesHandler getInitialPropertiesHandler() {
        return get(InitialPropertiesHandler.class);
    }

    /**
     * Gets the {@link Poller} singleton.
     *
     * @return the {@link Poller} singleton
     */
    public Poller getPoller() {
        return get(Poller.class);
    }

}
