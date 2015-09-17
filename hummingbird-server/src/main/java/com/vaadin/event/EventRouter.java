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
    private static ConcurrentHashMap<Class<? extends EventObject>, Class<? extends EventListener>> eventTypeToListenerType = new ConcurrentHashMap<>();

    @Override
    public <T extends EventListener> void addListener(Class<T> listenerType,
            T listener) {
        Class<? extends EventObject> eventType = getEventType(listenerType);
        List<EventListener> eventListeners = listeners
                .computeIfAbsent(eventType, e -> {
                    return new ArrayList<>();
                });
        eventTypeToListenerType.put(eventType, listenerType);
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends EventObject> getEventType(
            Class<? extends EventListener> listenerType) {
        return (Class<? extends EventObject>) getListenerMethod(listenerType)
                .getParameterTypes()[0];
    }

    /*
     * Removes all registered listeners matching the given parameters. Don't add
     * a JavaDoc comment here, we use the default documentation from implemented
     * interface.
     */
    @Override
    public <T extends EventListener> void removeListener(Class<T> listenerType,
            T listener) {
        Class<? extends EventObject> eventType = getEventType(listenerType);
        List<EventListener> eventListeners = listeners.get(eventType);
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
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners == null) {
            return;
        }

        Class<? extends EventListener> listenerType = eventTypeToListenerType
                .get(eventType);
        if (listenerType == null) {
            // No listeners have been registered for this event type
            return;
        }

        // Make a copy of the listener list to allow listeners to be added
        // inside listener methods (#3605)
        EventListener[] listenerArray = eventListeners
                .toArray(new EventListener[eventListeners.size()]);
        for (EventListener l : listenerArray) {
            try {
                Method listenerMethod = getListenerMethod(eventType,
                        listenerType);
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

    private static transient Map<Class<? extends EventListener>, Method> listenerMethods = new ConcurrentHashMap<>();

    private Method getListenerMethod(
            Class<? extends EventListener> listenerInterface) {
        if (listenerMethods.containsKey(listenerInterface)) {
            return listenerMethods.get(listenerInterface);
        }

        if (!listenerInterface.isInterface()) {
            throw new IllegalArgumentException("The listener type "
                    + listenerInterface.getName() + " is not an interface");
        }

        Method listenerMethod;
        List<Method> methods = Arrays
                .stream(listenerInterface.getDeclaredMethods())
                .filter(m -> !Modifier.isVolatile(m.getModifiers()))
                .collect(Collectors.toList());

        if (methods.size() != 1) {
            throw new IllegalArgumentException(
                    "The listener type " + listenerInterface.getName()
                            + " contains " + methods.size()
                            + " methods, but must contain exactly one");
        }

        listenerMethod = methods.get(0);
        if (listenerMethod.getParameterCount() != 1 || !EventObject.class
                .isAssignableFrom(listenerMethod.getParameterTypes()[0])) {
            throw new IllegalArgumentException("The listener type "
                    + listenerInterface.getName()
                    + " must contain exactly one method, which takes a sub class of "
                    + EventObject.class.getSimpleName() + " as its parameter");
        }
        listenerMethods.put(listenerInterface, listenerMethod);
        return listenerMethod;

    }

    private Method getListenerMethod(Class<? extends EventObject> eventType,
            Class<? extends EventListener> listenerType) {
        Method listenerMethod = getListenerMethod(listenerType);
        if (listenerMethod.getParameterTypes()[0] != eventType) {
            throw new IllegalArgumentException(
                    "The listener class " + listenerType.getName()
                            + " must contain exactly one method, which takes "
                            + eventType.getName() + " as its parameter");
        }
        return listenerMethod;
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
    public boolean hasListeners(Class<? extends EventListener> listenerType) {
        List<EventListener> eventListeners = listeners
                .get(getEventType(listenerType));
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
    public <T extends EventListener> Collection<T> getListeners(
            Class<T> listenerType) {
        @SuppressWarnings("unchecked")
        List<T> eventListeners = (List<T>) listeners
                .get(getEventType(listenerType));
        if (eventListeners == null) {
            return Collections.emptyList();
        }

        return new ArrayList<T>(eventListeners);
    }

    private static Logger getLogger() {
        return Logger.getLogger(EventRouter.class.getName());
    }

}
