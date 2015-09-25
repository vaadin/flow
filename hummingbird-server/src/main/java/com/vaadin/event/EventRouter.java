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

package com.vaadin.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;

/**
 * <code>EventRouter</code> class implementing the event listening model. For
 * more information on the event model see the {@link com.vaadin.event package
 * documentation}.
 *
 * @author Vaadin Ltd.
 */
@SuppressWarnings("serial")
public class EventRouter implements EventSource {

    /**
     * List of registered listeners.
     *
     * Never contains empty lists. If there are no listeners remaining, the
     * event type is removed
     */
    private LinkedHashMap<Class<? extends EventObject>, List<EventListener>> listeners = new LinkedHashMap<>();

    private static transient Map<Class<? extends EventObject>, Method> listenerMethods = new ConcurrentHashMap<>();

    @Override
    public void addListener(Class<? extends EventObject> eventType,
            EventListener listener) {
        // Class<? extends EventObject> eventType = getEventType(listenerType);
        // Class<EventListener> listenerType = getListenerType(eventType);
        List<EventListener> eventListeners = listeners
                .computeIfAbsent(eventType, e -> {
                    return new ArrayList<>();
                });
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    @Override
    public void removeListener(Class<? extends EventObject> eventType,
            EventListener listener) {
        List<EventListener> eventListeners = getRegisteredListeners(eventType);
        if (eventListeners == null) {
            return;
        }
        eventListeners.remove(listener);
        if (eventListeners.isEmpty()) {
            listeners.remove(eventType);
        }
    }

    /**
     * Removes all listeners from event router.
     */
    public void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Sends an event to all registered listeners. The listeners will decide if
     * the activation method should be called or not.
     *
     * @param event
     *            the Event to be sent to all listeners.
     */
    @Override
    public void fireEvent(EventObject event) {
        fireEvent(event, null);
    }

    /**
     * Sends an event to all registered listeners. The listeners will decide if
     * the activation method should be called or not.
     * <p>
     * If an error handler is set, the processing of other listeners will
     * continue after the error handler method call unless the error handler
     * itself throws an exception.
     *
     * @param event
     *            the Event to be sent to all listeners.
     * @param errorHandler
     *            error handler to use to handle any exceptions thrown by
     *            listeners or null to let the exception propagate to the
     *            caller, preventing further listener calls
     */
    public void fireEvent(EventObject event, ErrorHandler errorHandler) {
        Class<? extends EventObject> eventType = event.getClass();
        List<EventListener> eventListeners = getRegisteredListeners(eventType);

        if (eventListeners == null) {
            return;
        }

        // Make a copy of the listener list to allow listeners to be added
        // inside listener methods (#3605)
        EventListener[] listenerArray = eventListeners
                .toArray(new EventListener[eventListeners.size()]);
        for (EventListener l : listenerArray) {
            try {
                Method listenerMethod = getListenerMethod(eventType,
                        l.getClass());
                listenerMethod.invoke(l, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                getLogger().log(Level.SEVERE,
                        "Error sending event of type " + eventType.getName()
                                + " to listener of type " + l.getClass(),
                        e);
            } catch (RuntimeException e) {
                if (errorHandler != null) {
                    errorHandler.error(new ErrorEvent(e));
                } else {
                    throw e;
                }
            }

        }

    }

    private Method getListenerMethod(Class<? extends EventObject> eventType,
            Class<? extends EventListener> listenerType) {

        if (listenerMethods.containsKey(eventType)) {
            return listenerMethods.get(eventType);
        }

        Method listenerMethod;
        List<Method> methods = Arrays.stream(listenerType.getMethods())
                .filter(m -> m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == eventType
                        && !Modifier.isVolatile(m.getModifiers()))
                .collect(Collectors.toList());

        if (methods.size() != 1) {
            throw new IllegalArgumentException("The listener type "
                    + listenerType.getName() + " contains " + methods.size()
                    + " methods taking " + eventType.getName()
                    + " as a parameter, but must contain exactly one");
        }

        listenerMethod = methods.get(0);
        listenerMethod = findInterfaceMethod(listenerMethod);
        listenerMethods.put(eventType, listenerMethod);
        return listenerMethod;
    }

    private Method findInterfaceMethod(Method listenerMethod) {
        Class<?> listenerClass = listenerMethod.getDeclaringClass();
        for (Class<?> c : listenerMethod.getDeclaringClass().getInterfaces()) {
            Method interfaceMethod;
            try {
                interfaceMethod = c.getMethod(listenerMethod.getName(),
                        listenerMethod.getParameterTypes());
                return interfaceMethod;
            } catch (NoSuchMethodException | SecurityException e) {
            }
        }

        // Not found in any interface, try superclass
        Method superMethod;
        try {
            superMethod = listenerClass.getSuperclass().getMethod(
                    listenerMethod.getName(),
                    listenerMethod.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException e) {
            // Should not happen, unless the listener method is for some reason
            // defined in a class and not an interface
            return null;
        }

        return findInterfaceMethod(superMethod);

    }

    /**
     * Checks if the given Event type is listened by a listener registered to
     * this router.
     *
     * @param eventType
     *            the event type to be checked
     * @return true if a listener is registered for the given event type
     */
    @Override
    public boolean hasListeners(Class<? extends EventObject> eventType) {
        List<?> eventListeners = getRegisteredListeners(eventType);
        return (eventListeners != null);
    }

    /**
     * Returns all listeners that match or extend the given event type.
     *
     * @param eventType
     *            The type of event to return listeners for.
     * @return A collection with all registered listeners. Empty if no listeners
     *         are found.
     */
    @Override
    public Collection<EventListener> getListeners(
            Class<? extends EventObject> eventType) {
        List<EventListener> eventListeners = getRegisteredListeners(eventType);
        if (eventListeners == null) {
            return Collections.emptyList();
        }

        return new ArrayList<EventListener>(eventListeners);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List<EventListener> getRegisteredListeners(
            Class<? extends EventObject> eventType) {
        return listeners.get(eventType);
    }

    private static Logger getLogger() {
        return Logger.getLogger(EventRouter.class.getName());
    }

}
