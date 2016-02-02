/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import java.util.Objects;

import com.vaadin.client.hummingbird.reactive.ReactiveChangeListener;
import com.vaadin.client.hummingbird.reactive.ReactiveEventRouter;
import com.vaadin.client.hummingbird.reactive.ReactiveValue;

import elemental.events.EventRemover;

/**
 * A property in a map namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapProperty implements ReactiveValue {
    private final String name;
    private final MapNamespace namespace;

    private final ReactiveEventRouter<MapPropertyChangeListener, MapPropertyChangeEvent> eventRouter = new ReactiveEventRouter<MapPropertyChangeListener, MapPropertyChangeEvent>(
            this) {
        @Override
        protected MapPropertyChangeListener wrap(ReactiveChangeListener l) {
            return l::onChange;
        }

        @Override
        protected void dispatchEvent(MapPropertyChangeListener listener,
                MapPropertyChangeEvent event) {
            listener.onPropertyChange(event);
        }
    };

    private Object value;

    /**
     * Creates a new property.
     *
     * @param name
     *            the name of the property
     * @param namespace
     *            the namespace that the property belongs to
     */
    public MapProperty(String name, MapNamespace namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Gets the name of this property.
     *
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the namespace that this property belongs to.
     *
     * @return the namespace
     */
    public MapNamespace getNamespace() {
        return namespace;
    }

    /**
     * Gets the property value.
     *
     * @return the property value
     */
    public Object getValue() {
        eventRouter.registerRead();
        return value;
    }

    /**
     * Sets the property value. Changing the value fires a
     * {@link MapPropertyChangeEvent}.
     *
     * @see #addChangeListener(MapPropertyChangeListener)
     *
     * @param value
     *            the new property value
     */
    public void setValue(Object value) {
        Object oldValue = this.value;
        if (!Objects.equals(value, oldValue)) {
            this.value = value;
            eventRouter.fireEvent(
                    new MapPropertyChangeEvent(this, oldValue, value));
        }
    }

    /**
     * Adds a listener that gets notified when the values of this property
     * changes.
     *
     * @param listener
     *            the property change listener to add
     * @return an event remover for unregistering the listener
     */
    public EventRemover addChangeListener(MapPropertyChangeListener listener) {
        return eventRouter.addListener(listener);
    }

    @Override
    public EventRemover addReactiveChangeListener(ReactiveChangeListener listener) {
        return eventRouter.addReactiveListener(listener);
    }
}
