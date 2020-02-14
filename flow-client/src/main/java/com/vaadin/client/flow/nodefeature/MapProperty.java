/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import java.util.Map;
import java.util.Objects;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.reactive.ReactiveEventRouter;
import com.vaadin.client.flow.reactive.ReactiveValue;
import com.vaadin.client.flow.reactive.ReactiveValueChangeListener;

import elemental.events.EventRemover;

/**
 * A property in a node map.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapProperty implements ReactiveValue {
    private final String name;
    private final NodeMap map;

    /**
     * Indicates that the server update is in progress. While this is true we
     * don't accept any changes via {@link #syncToServer(Object)} method.
     */
    private boolean isServerUpdate;

    private static final Runnable NO_OP = () -> {
    };

    private final ReactiveEventRouter<MapPropertyChangeListener, MapPropertyChangeEvent> eventRouter = new ReactiveEventRouter<MapPropertyChangeListener, MapPropertyChangeEvent>(
            this) {
        @Override
        protected MapPropertyChangeListener wrap(
                ReactiveValueChangeListener listener) {
            return listener::onValueChange;
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
        // mark as server update is in progress
        isServerUpdate = true;
        doSetValue(value);
        // unmark server update in the end of flush meaning in the end of the
        // current server request processing
        Reactive.addPostFlushListener(() -> isServerUpdate = false);
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
            // mark as server update is in progress
            isServerUpdate = true;
            updateValue(null, false);
            // unmark server update in the end of flush meaning in the end of
            // the current server request processing
            Reactive.addPostFlushListener(() -> isServerUpdate = false);
        }
    }

    private void doSetValue(Object value) {
        if (hasValue && Objects.equals(value, this.value)) {
            // Nothing to do
            return;
        }
        updateValue(value, true);
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
    public EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener) {
        return eventRouter.addReactiveListener(reactiveValueChangeListener);
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

    /**
     * Sets the value of this property and synchronizes the value to the server.
     *
     * @param newValue
     *            the new value to set.
     * @see #getSyncToServerCommand(Object)
     */
    public void syncToServer(Object newValue) {
        getSyncToServerCommand(newValue).run();
    }

    /**
     * Sets the value of this property and returns a synch to server command.
     *
     * @param newValue
     *            the new value to set.
     * @see #syncToServer(Object)
     */
    public Runnable getSyncToServerCommand(Object newValue) {
        Object currentValue = hasValue() ? getValue() : null;

        if (Objects.equals(newValue, currentValue)) {
            // in case we are here with the same value that has been set from
            // the server then we unlock client side updates already here via
            // unmarking the server update flag. It allows another client side
            // potential change for the same property being propagated to the
            // server once the server value is set successfully (e.g. mutation
            // the same property from its observer).
            isServerUpdate = false;
        }
        if (!(Objects.equals(newValue, currentValue) && hasValue())
                && !isServerUpdate) {
            StateNode node = getMap().getNode();
            StateTree tree = node.getTree();
            if (tree.isActive(node)) {
                doSetValue(newValue);

                return () -> tree.sendNodePropertySyncToServer(this);
            } else {
                /*
                 * Fire an fake event to reset the property value back in the
                 * DOM element: we don't know how exactly set this property but
                 * it has to be set to the property value because of listener
                 * added to the property during binding.
                 */
                eventRouter.fireEvent(new MapPropertyChangeEvent(this,
                        currentValue, currentValue));
                // Flush is needed because we are out of normal lifecycle which
                // call the flush() automatically.
                Reactive.flush();
            }
        }
        return NO_OP;
    }
}
