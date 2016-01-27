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
package com.vaadin.server;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.logging.Logger;

import com.vaadin.event.EventRouter;
import com.vaadin.event.MethodEventSource;
import com.vaadin.shared.communication.MethodInvocation;
import com.vaadin.shared.communication.ServerRpc;
import com.vaadin.shared.communication.SharedState;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.UI;

/**
 * TODO Integrate with AbstractComponent
 */
public abstract class AbstractClientConnector
        implements ClientConnector, MethodEventSource {

    /**
     * Shared state object to be communicated from the server to the client when
     * modified.
     */
    private SharedState sharedState;
    private Class<? extends SharedState> stateType;

    private String connectorId;

    /**
     * The EventRouter used for the event model.
     */
    private EventRouter eventRouter = null;

    private ErrorHandler errorHandler = null;

    @Override
    public void addAttachListener(AttachListener listener) {
        addListener(AttachEvent.ATTACH_EVENT_IDENTIFIER, AttachEvent.class,
                listener, AttachListener.attachMethod);
    }

    @Override
    public void removeAttachListener(AttachListener listener) {
        removeListener(AttachEvent.ATTACH_EVENT_IDENTIFIER, AttachEvent.class,
                listener);
    }

    @Override
    public void addDetachListener(DetachListener listener) {
        addListener(DetachEvent.DETACH_EVENT_IDENTIFIER, DetachEvent.class,
                listener, DetachListener.detachMethod);
    }

    @Override
    public void removeDetachListener(DetachListener listener) {
        removeListener(DetachEvent.DETACH_EVENT_IDENTIFIER, DetachEvent.class,
                listener);
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
    @Deprecated
    protected <T extends ServerRpc> void registerRpc(T implementation,
            Class<T> rpcInterfaceType) {
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

    public Class<? extends SharedState> getStateType() {
        // Lazy load because finding type can be expensive because of the
        // exceptions flying around
        if (stateType == null) {
            // Cache because we don't need to do this once per instance
            stateType = findStateType();
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
    public static Iterable<? extends ClientConnector> getAllChildrenIterable(
            final ClientConnector connector) {

        boolean hasComponents = connector instanceof HasComponents;
        if (!hasComponents) {
            // If has neither component nor extensions, return immutable empty
            // list as iterable.
            return Collections.emptyList();
        }
        // only components
        return (HasComponents) connector;
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
    public void attach() {
        markAsDirty();

        getUI().getConnectorTracker().registerConnector(this);

        for (ClientConnector connector : getAllChildrenIterable(this)) {
            connector.attach();
        }

        fireEvent(new AttachEvent((Component) this));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The {@link #getSession()} and {@link #getUI()} methods might return
     * <code>null</code> after this method is called.
     * </p>
     */
    @Override
    public void detach() {
        for (ClientConnector connector : getAllChildrenIterable(this)) {
            connector.detach();
        }

        fireEvent(new DetachEvent((Component) this));

        getUI().getConnectorTracker().unregisterConnector(this);
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

    /* Listener code starts. Should be refactored. */

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     *
     * <p>
     * This method additionally informs the event-api to route events with the
     * given eventIdentifier to the components handleEvent function call.
     * </p>
     *
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     *
     * @param eventIdentifier
     *            the identifier of the event to listen for
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     *
     * @since 6.2
     */
    protected void addListener(String eventIdentifier, Class<?> eventType,
            Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        boolean needRepaint = !eventRouter.hasListeners(eventType);
        eventRouter.addListener(eventType, target, method);
    }

    /**
     * Checks if the given {@link ComponentEvent} type is listened for this
     * component.
     *
     * @param eventType
     *            the event type to be checked
     * @return true if a listener is registered for the given event type
     */
    protected boolean hasListeners(Class<?> eventType) {
        return eventRouter != null && eventRouter.hasListeners(eventType);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     *
     * <p>
     * This method additionally informs the event-api to stop routing events
     * with the given eventIdentifier to the components handleEvent function
     * call.
     * </p>
     *
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     *
     * @param eventIdentifier
     *            the identifier of the event to stop listening for
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     *
     * @since 6.2
     */
    protected void removeListener(String eventIdentifier, Class<?> eventType,
            Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
        }
    }

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     *
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     *
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     *
     */
    @Override
    public void addListener(Class<?> eventType, Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, method);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     *
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     *
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     */
    @Override
    public void removeListener(Class<?> eventType, Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
        }
    }

    /**
     * Removes one registered listener method. The given method owned by the
     * given object will no longer be called when the specified events are
     * generated by this component.
     *
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     *
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            target object that has registered to listen to events of type
     *            <code>eventType</code> with one or more methods.
     * @param method
     *            the method owned by <code>target</code> that's registered to
     *            listen to events of type <code>eventType</code>.
     */
    @Override
    public void removeListener(Class<?> eventType, Object target,
            Method method) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, method);
        }
    }

    /**
     * Returns all listeners that are registered for the given event type or one
     * of its subclasses.
     *
     * @param eventType
     *            The type of event to return listeners for.
     * @return A collection with all registered listeners. Empty if no listeners
     *         are found.
     */
    public Collection<?> getListeners(Class<?> eventType) {
        if (eventRouter == null) {
            return Collections.EMPTY_LIST;
        }

        return eventRouter.getListeners(eventType);
    }

    /**
     * Sends the event to all listeners.
     *
     * @param event
     *            the Event to be sent to all listeners.
     */
    protected void fireEvent(EventObject event) {
        if (eventRouter != null) {
            eventRouter.fireEvent(event);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.ClientConnector#getErrorHandler()
     */
    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.server.ClientConnector#setErrorHandler(com.vaadin.server.
     * ErrorHandler)
     */
    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void handleRpc(MethodInvocation invocation) {
        throw new RuntimeException(
                "Rpc received but handleRpc is not overridden");
    }
}
