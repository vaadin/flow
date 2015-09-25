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

import java.io.Serializable;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;

/**
 * Interface for classes supporting registration of event handlers and routing
 * of events.
 *
 * @author Vaadin Ltd.
 */
public interface EventSource extends Serializable {

    /**
     * <p>
     * Registers a new event listener.
     * </p>
     * An event listener must be a functional interface, i.e. contains one
     * method. The method has one parameter, which is the event type.
     *
     * @param listener
     *            the event listener which contains an listener method.
     */
    public void addListener(Class<? extends EventObject> eventType,
            EventListener listener);

    /**
     * Removes the given listener.
     *
     * @param listener
     *            the listener to remove
     */
    public void removeListener(Class<? extends EventObject> eventType,
            EventListener listener);

    /**
     * Returns all registered listeners for the given event type
     *
     * @param eventType
     *            the type of event
     * @return A collection with all registered listeners. Empty if no listeners
     *         are found.
     */
    public Collection<EventListener> getListeners(
            Class<? extends EventObject> eventType);

    /**
     * Checks if any listeners of the given type are registered.
     *
     * @param eventType
     *            the type of event
     * @return true if there is at least one listener registered, false
     *         otherwise
     */
    public boolean hasListeners(Class<? extends EventObject> eventType);

    /**
     * Sends the given event to all registered listeners
     *
     * @param eventObject
     *            the event to be sent to all listeners.
     */
    public void fireEvent(EventObject eventObject);

}
