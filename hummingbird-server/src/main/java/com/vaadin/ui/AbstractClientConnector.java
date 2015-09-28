/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.vaadin.server.ClientConnector;
import com.vaadin.server.ClientMethodInvocation;
import com.vaadin.server.ConnectorResource;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.LegacyCommunicationManager;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.ServerRpcManager;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.ClientRpc;
import com.vaadin.shared.communication.ServerRpc;
import com.vaadin.shared.communication.SharedState;
import com.vaadin.ui.UI.Root;

import elemental.json.JsonObject;

/**
 * An abstract base class for ClientConnector implementations. This class
 * provides all the basic functionality required for connectors.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
// TODO Remove class
@Deprecated
public abstract class AbstractClientConnector extends AbstractHasElement
        implements ClientConnector {
    /**
     * A map from client to server RPC interface class name to the RPC call
     * manager that handles incoming RPC calls for that interface.
     */
    private Map<String, ServerRpcManager<?>> rpcManagerMap = new HashMap<String, ServerRpcManager<?>>();

    /**
     * A map from server to client RPC interface class to the RPC proxy that
     * sends ourgoing RPC calls for that interface.
     */
    private Map<Class<?>, ClientRpc> rpcProxyMap = new HashMap<Class<?>, ClientRpc>();

    /**
     * Shared state object to be communicated from the server to the client when
     * modified.
     */
    private SharedState sharedState;

    private Class<? extends SharedState> stateType;

    /**
     * Pending RPC method invocations to be sent.
     */
    private ArrayList<ClientMethodInvocation> pendingInvocations = new ArrayList<ClientMethodInvocation>();

    private String connectorId;

    private static final ConcurrentHashMap<Class<? extends AbstractClientConnector>, Class<? extends SharedState>> stateTypeCache = new ConcurrentHashMap<Class<? extends AbstractClientConnector>, Class<? extends SharedState>>();

    public AbstractClientConnector() {
        super();
    }

    public AbstractClientConnector(String tagName) {
        super(tagName);
    }

    /* Documentation copied from interface */
    @Override
    public void markAsDirty() {
        assert getSession() == null
                || getSession().hasLock() : buildLockAssertMessage(
                        "markAsDirty()");
        UI uI = getUI();
        if (uI != null) {
            uI.getConnectorTracker().markDirty(this);
        }
    }

    private String buildLockAssertMessage(String method) {
        if (VaadinService.isOtherSessionLocked(getSession())) {
            return "The session of this connecor is not locked, but there is another session that is locked. "
                    + "This might be caused by accidentally using a connector that belongs to another session.";
        } else {
            return "Session must be locked when " + method + " is called";
        }
    }

    /**
     * Registers an RPC interface implementation for this component.
     *
     * A component can listen to multiple RPC interfaces, and subclasses can
     * register additional implementations.
     *
     * @since 7.0
     *
     * @param implementation
     *            RPC interface implementation
     * @param rpcInterfaceType
     *            RPC interface class for which the implementation should be
     *            registered
     */
    protected <T extends ServerRpc> void registerRpc(T implementation,
            Class<T> rpcInterfaceType) {
        rpcManagerMap.put(rpcInterfaceType.getName(),
                new ServerRpcManager<T>(implementation, rpcInterfaceType));
    }

    /**
     * Registers an RPC interface implementation for this component.
     *
     * A component can listen to multiple RPC interfaces, and subclasses can
     * register additional implementations.
     *
     * @since 7.0
     *
     * @param implementation
     *            RPC interface implementation. Also used to deduce the type.
     */
    protected <T extends ServerRpc> void registerRpc(T implementation) {
        // Search upwards until an interface is found. It must be found as T
        // extends ServerRpc
        Class<?> cls = implementation.getClass();
        Class<ServerRpc> serverRpcClass = getServerRpcInterface(cls);

        while (cls != null && serverRpcClass == null) {
            cls = cls.getSuperclass();
            serverRpcClass = getServerRpcInterface(cls);
        }

        if (serverRpcClass == null) {
            throw new RuntimeException(
                    "No interface T extends ServerRpc found in the class hierarchy.");
        }

        registerRpc(implementation, serverRpcClass);
    }

    @SuppressWarnings("unchecked")
    private Class<ServerRpc> getServerRpcInterface(
            Class<?> implementationClass) {
        Class<ServerRpc> serverRpcClass = null;
        if (implementationClass != null) {
            for (Class<?> candidateInterface : implementationClass
                    .getInterfaces()) {
                if (ServerRpc.class.isAssignableFrom(candidateInterface)) {
                    if (serverRpcClass != null) {
                        throw new RuntimeException(
                                "Use registerRpc(T implementation, Class<T> rpcInterfaceType) if the Rpc implementation implements more than one interface");
                    }
                    serverRpcClass = (Class<ServerRpc>) candidateInterface;
                }
            }
        }
        return serverRpcClass;
    }

    /**
     * Returns the shared state for this connector. The shared state object is
     * shared between the server connector and the client connector. Changes are
     * only communicated from the server to the client and not in the other
     * direction.
     * <p>
     * As a side effect, marks the connector dirty so any changes done to the
     * state will be sent to the client. Use {@code getState(false)} to avoid
     * marking the connector as dirty.
     * </p>
     *
     * @return The shared state for this connector. Never null.
     */
    protected SharedState getState() {
        return getState(true);
    }

    /**
     * Returns the shared state for this connector.
     *
     * @param markAsDirty
     *            true if the connector should automatically be marked dirty,
     *            false otherwise
     *
     * @return The shared state for this connector. Never null.
     * @see #getState()
     */
    protected SharedState getState(boolean markAsDirty) {
        assert getSession() == null
                || getSession().hasLock() : buildLockAssertMessage(
                        "getState()");

        if (null == sharedState) {
            sharedState = createState();
        }
        if (markAsDirty) {
            UI ui = getUI();
            if (ui != null && !ui.getConnectorTracker().isDirty(this)
                    && !ui.getConnectorTracker().isWritingResponse()) {
                ui.getConnectorTracker().markDirty(this);
            }
        }
        return sharedState;
    }

    @Override
    public JsonObject encodeState() {
        return LegacyCommunicationManager.encodeState(this, getState(false));
    }

    /**
     * Creates the shared state bean to be used in server to client
     * communication.
     * <p>
     * By default a state object of the defined return type of
     * {@link #getState()} is created. Subclasses can override this method and
     * return a new instance of the correct state class but this should rarely
     * be necessary.
     * </p>
     * <p>
     * No configuration of the values of the state should be performed in
     * {@link #createState()}.
     *
     * @since 7.0
     *
     * @return new shared state object
     */
    protected SharedState createState() {
        try {
            return getStateType().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating state of type "
                    + getStateType().getName() + " for " + getClass().getName(),
                    e);
        }
    }

    @Override
    public Class<? extends SharedState> getStateType() {
        // Lazy load because finding type can be expensive because of the
        // exceptions flying around
        if (stateType == null) {
            // Cache because we don't need to do this once per instance
            stateType = stateTypeCache.get(this.getClass());
            if (stateType == null) {
                stateType = findStateType();
                stateTypeCache.put(this.getClass(), stateType);
            }
        }

        return stateType;
    }

    private Class<? extends SharedState> findStateType() {
        try {
            Class<?> class1 = getClass();
            while (class1 != null) {
                try {
                    Method m = class1.getDeclaredMethod("getState",
                            (Class[]) null);
                    Class<?> type = m.getReturnType();
                    if (!m.isSynthetic()) {
                        return type.asSubclass(SharedState.class);
                    }
                } catch (NoSuchMethodException nsme) {
                }
                // Try in superclass instead
                class1 = class1.getSuperclass();
            }
            throw new NoSuchMethodException(
                    getClass().getCanonicalName() + ".getState()");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error finding state type for " + getClass().getName(), e);
        }
    }

    /**
     * Returns an RPC proxy for a given server to client RPC interface for this
     * component.
     *
     * TODO more javadoc, subclasses, ...
     *
     * @param rpcInterface
     *            RPC interface type
     *
     * @since 7.0
     */
    @Deprecated
    protected <T extends ClientRpc> T getRpcProxy(final Class<T> rpcInterface) {
        // create, initialize and return a dynamic proxy for RPC
        try {
            if (!rpcProxyMap.containsKey(rpcInterface)) {
                Class<?> proxyClass = Proxy.getProxyClass(
                        rpcInterface.getClassLoader(), rpcInterface);
                Constructor<?> constructor = proxyClass
                        .getConstructor(InvocationHandler.class);
                T rpcProxy = rpcInterface.cast(constructor
                        .newInstance(new RpcInvocationHandler(rpcInterface)));
                // cache the proxy
                rpcProxyMap.put(rpcInterface, rpcProxy);
            }
            return (T) rpcProxyMap.get(rpcInterface);
        } catch (Exception e) {
            // TODO exception handling?
            throw new RuntimeException(e);
        }
    }

    private class RpcInvocationHandler
            implements InvocationHandler, Serializable {

        private String rpcInterfaceName;

        public RpcInvocationHandler(Class<?> rpcInterface) {
            rpcInterfaceName = rpcInterface.getName().replaceAll("\\$", ".");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                // Don't add Object methods such as toString and hashCode as
                // invocations
                return method.invoke(this, args);
            }
            addMethodInvocationToQueue(rpcInterfaceName, method, args);
            return null;
        }

    }

    /**
     * For internal use: adds a method invocation to the pending RPC call queue.
     *
     * @param interfaceName
     *            RPC interface name
     * @param method
     *            RPC method
     * @param parameters
     *            RPC all parameters
     *
     * @since 7.0
     */
    protected void addMethodInvocationToQueue(String interfaceName,
            Method method, Object[] parameters) {
        // add to queue
        pendingInvocations.add(new ClientMethodInvocation(this, interfaceName,
                method, parameters));
        // TODO no need to do full repaint if only RPC calls
        markAsDirty();
    }

    @Override
    public ServerRpcManager<?> getRpcManager(String rpcInterfaceName) {
        return rpcManagerMap.get(rpcInterfaceName);
    }

    @Override
    public List<ClientMethodInvocation> retrievePendingRpcCalls() {
        if (pendingInvocations.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ClientMethodInvocation> result = pendingInvocations;
            pendingInvocations = new ArrayList<ClientMethodInvocation>();
            return Collections.unmodifiableList(result);
        }
    }

    @Override
    public String getConnectorId() {
        if (connectorId == null) {
            if (getSession() == null) {
                throw new RuntimeException(
                        "Component must be attached to a session when getConnectorId() is called for the first time");
            }
            connectorId = getSession().createConnectorId(this);
        }
        return connectorId;
    }

    /**
     * Finds the {@link VaadinSession} to which this connector belongs. If the
     * connector has not been attached, <code>null</code> is returned.
     *
     * @return The connector's session, or <code>null</code> if not attached
     */
    protected VaadinSession getSession() {
        UI uI = getUI();
        if (uI == null) {
            return null;
        } else {
            return uI.getSession();
        }
    }

    /**
     * Finds a UI ancestor of this connector. <code>null</code> is returned if
     * no UI ancestor is found (typically because the connector is not attached
     * to a proper hierarchy).
     *
     * @return the UI ancestor of this connector, or <code>null</code> if none
     *         is found.
     */
    @Override
    public UI getUI() {
        ClientConnector connector = this;
        while (connector != null) {
            if (connector instanceof UI) {
                return (UI) connector;
            }
            if (connector instanceof Root) {
                return ((Root) connector).getUI();
            }
            connector = connector.getParent();
        }
        return null;
    }

    private static Logger getLogger() {
        return Logger.getLogger(AbstractClientConnector.class.getName());
    }

    @Override
    public void markAsDirtyRecursive() {
        markAsDirty();

        for (ClientConnector connector : getAllChildrenIterable(this)) {
            connector.markAsDirtyRecursive();
        }
    }

    /**
     * Get an Iterable for iterating over all child connectors, including both
     * extensions and child components.
     *
     * @param connector
     *            the connector to get children for
     * @return an Iterable giving all child connectors.
     */
    public static Iterable<? extends Component> getAllChildrenIterable(
            final ClientConnector connector) {

        boolean hasComponents = connector instanceof HasComponents;
        if (!hasComponents) {
            // If has neither component nor extensions, return immutable empty
            // list as iterable.
            return Collections.emptyList();
        }
        if (hasComponents) {
            // only components
            return (HasComponents) connector;
        }

        // combine the iterators of extensions and components to a new iterable.
        final Iterator<Component> componentsIterator = ((HasComponents) connector)
                .iterator();
        Iterable<Component> combinedIterable = new Iterable<Component>() {

            @Override
            public Iterator<Component> iterator() {
                return new Iterator<Component>() {

                    @Override
                    public boolean hasNext() {
                        return componentsIterator.hasNext();
                    }

                    @Override
                    public Component next() {
                        if (componentsIterator.hasNext()) {
                            return componentsIterator.next();
                        }
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }
        };
        return combinedIterable;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.ClientConnector#isAttached()
     */
    @Override
    public boolean isAttached() {
        return getSession() != null;
    }

    @Override
    public boolean isConnectorEnabled() {
        if (getParent() == null) {
            // No parent -> the component cannot receive updates from the client
            return false;
        } else {
            return getParent().isConnectorEnabled();
        }
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        // Do nothing by default
    }

    @Override
    public boolean handleConnectorRequest(VaadinRequest request,
            VaadinResponse response, String path) throws IOException {
        DownloadStream stream = null;
        String[] parts = path.split("/", 2);
        String key = parts[0];

        VaadinSession session = getSession();
        session.lock();
        try {
            ConnectorResource resource = (ConnectorResource) getResource(key);
            if (resource == null) {
                return false;
            }
            stream = resource.getStream();
        } finally {
            session.unlock();
        }
        stream.writeResponse(request, response);
        return true;
    }

    /**
     * Gets a resource defined using {@link #setResource(String, Resource)} with
     * the corresponding key.
     *
     * @param key
     *            the string identifier of the resource
     * @return a resource, or <code>null</code> if there's no resource
     *         associated with the given key
     *
     * @see #setResource(String, Resource)
     */
    protected Resource getResource(String key) {
        return ResourceReference
                .getResource(getState(false).resources.get(key));
    }

    /**
     * Registers a resource with this connector using the given key. This will
     * make the URL for retrieving the resource available to the client-side
     * connector using
     * {@link com.vaadin.terminal.gwt.client.ui.AbstractConnector#getResourceUrl(String)}
     * with the same key.
     *
     * @param key
     *            the string key to associate the resource with
     * @param resource
     *            the resource to set, or <code>null</code> to clear a previous
     *            association.
     */
    protected void setResource(String key, Resource resource) {
        ResourceReference resourceReference = ResourceReference.create(resource,
                this, key);

        if (resourceReference == null) {
            getState().resources.remove(key);
        } else {
            getState().resources.put(key, resourceReference);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        /*
         * This equals method must return true when we're comparing an object to
         * its proxy. This happens a lot with CDI (and possibly Spring) when
         * we're injecting Components. See #14639
         */
        if (obj instanceof AbstractClientConnector) {
            AbstractClientConnector connector = (AbstractClientConnector) obj;
            return connector.isThis(this);
        }
        return false;
    }

    /**
     * For internal use only, may be changed or removed in future versions.
     * <p>
     * This method must be protected, because otherwise it will not be redefined
     * by the proxy to actually be called on the underlying instance.
     * <p>
     * See #14639
     *
     * @deprecated only defined for framework hacks, do not use.
     */
    @Deprecated
    protected boolean isThis(Object that) {
        return this == that;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
