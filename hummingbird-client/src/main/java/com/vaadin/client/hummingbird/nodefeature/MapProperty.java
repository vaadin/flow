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
package com.vaadin.client.hummingbird.nodefeature;

import java.util.Map;
import java.util.Objects;

import com.vaadin.client.hummingbird.reactive.ReactiveChangeListener;
import com.vaadin.client.hummingbird.reactive.ReactiveEventRouter;
import com.vaadin.client.hummingbird.reactive.ReactiveValue;

import elemental.events.EventRemover;

/**
 * A property in a node map.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapProperty implements ReactiveValue {
    private final String name;
    private final NodeMap map;

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
    private boolean hasValue = false;

    /**
     * Creates a new property.
     *
     * @param name
     *            the name of the property
     * @param map
     *            the map that the property belongs to
     */
    public MapProperty(String name, NodeMap map) {
        this.name = name;
        this.map = map;
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
     * Gets the map that this property belongs to.
     *
     * @return the map
     */
    public NodeMap getMap() {
        return map;
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
     * Checks whether this property has a value. A property has a value if
     * {@link #setValue(Object)} has been invoked after the property was created
     * or {@link #removeValue()} was invoked.
     *
     * @see #removeValue()
     *
     * @return <code>true</code> if the property has a value, <code>false</code>
     *         if the property has no value.
     */
    public boolean hasValue() {
        eventRouter.registerRead();
        return hasValue;
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
        if (hasValue && Objects.equals(value, this.value)) {
            // Nothing to do
            return;
        }
        updateValue(value, true);
    }

    /**
     * Removes the value of this property so that {@link #hasValue()} will
     * return <code>false</code> and {@link #getValue()} will return
     * <code>null</code> until the next time {@link #setValue(Object)} is run. A
     * {@link MapPropertyChangeEvent} will be fired if this property has a
     * value.
     * <p>
     * Once a property has been created, it can no longer be removed from its
     * map. The same semantics as e.g. {@link Map#remove(Object)} is instead
     * provided by marking the value of the property as removed to distinguish
     * it from assigning <code>null</code> as the value.
     */
    public void removeValue() {
        if (hasValue) {
            updateValue(null, false);
        }
    }

    private void updateValue(Object value, boolean hasValue) {
        Object oldValue = this.value;

        this.hasValue = hasValue;
        this.value = value;

        eventRouter
                .fireEvent(new MapPropertyChangeEvent(this, oldValue, value));
    }

    /**
     * Adds a listener that gets notified when the value of this property
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
    public EventRemover addReactiveChangeListener(
            ReactiveChangeListener listener) {
        return eventRouter.addReactiveListener(listener);
    }

    /**
     * Returns the value, or the given defaultValue if the property does not
     * have a value or the property value is null.
     *
     * @param defaultValue
     *            the default value
     * @return the value of the property or the default value if the property
     *         does not have a value or the property value is null
     */
    public int getValueOrDefault(int defaultValue) {
        if (hasValue()) {
            Object v = getValue();
            if (v == null) {
                return defaultValue;
            }
            return ((Double) v).intValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the value, or the given defaultValue if the property does not
     * have a value or the property value is null.
     *
     * @param defaultValue
     *            the default value
     * @return the value of the property or the default value if the property
     *         does not have a value or the property value is null
     */
    public boolean getValueOrDefault(boolean defaultValue) {
        if (hasValue()) {
            Object v = getValue();
            if (v == null) {
                return defaultValue;
            }
            return (boolean) v;
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the value, or the given defaultValue if the property does not
     * have a value or the property value is null.
     *
     * @param defaultValue
     *            the default value
     * @return the value of the property or the default value if the property
     *         does not have a value or the property value is null
     */
    public String getValueOrDefault(String defaultValue) {
        if (hasValue()) {
            Object v = getValue();
            if (v == null) {
                return defaultValue;
            }
            return (String) getValue();
        } else {
            return defaultValue;
        }
    }
}
