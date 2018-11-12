/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.templatemodel.AllowClientUpdates;

/**
 * Map for element property values.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementPropertyMap extends AbstractPropertyMap {

    private static final Set<String> forbiddenProperties = Stream
            .of("textContent", "classList", "className")
            .collect(Collectors.toSet());

    private Map<String, List<PropertyChangeListener>> listeners;

    private SerializablePredicate<String> updateFromClientFilter = null;

    private enum AllowUpdate {
        EXPLICITLY_ALLOW, EXPLICITLY_DISALLOW, NO_EXPLICIT_STATUS
    }

    /**
     * Creates a new element property map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ElementPropertyMap(StateNode node) {
        super(node);
    }

    /**
     * Updates a property value from the client and returns a Runnable for
     * firing the associated PropertyChangeEvent.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to store
     * @return a runnable for firing the deferred change event
     */
    public Runnable deferredUpdateFromClient(String key, Serializable value) {
        return doDeferredUpdateFromClient(key, value);
    }

    @Override
    public void setProperty(String name, Serializable value,
            boolean emitChange) {
        assert !forbiddenProperties.contains(name) : "Forbidden property name: "
                + name;

        super.setProperty(name, value, emitChange);
    }

    /**
     * Sets a property to the given value.
     *
     * @param name
     *            the property name
     * @param value
     *            the value, must be a string, a boolean, a double or
     *            <code>null</code>
     * @see #setProperty(String, Serializable, boolean)
     */
    public void setProperty(String name, Serializable value) {
        setProperty(name, value, true);
    }

    /**
     * Adds a property change listener.
     *
     * @param name
     *            the property name to add the listener for
     * @param listener
     *            listener to get notifications about property value changes
     * @return an event registration handle for removing the listener
     */
    public Registration addPropertyChangeListener(String name,
            PropertyChangeListener listener) {
        assert hasElement();
        Objects.requireNonNull(name, "Property name cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");

        List<PropertyChangeListener> propertyListeners;
        if (listeners == null) {
            propertyListeners = new ArrayList<>(1);
            listeners = Collections.singletonMap(name, propertyListeners);
        } else {
            if (listeners.size() == 1 && !(listeners instanceof HashMap)) {
                listeners = new HashMap<>(listeners);
            }
            propertyListeners = listeners.computeIfAbsent(name,
                    key -> new ArrayList<>(1));
        }

        propertyListeners.add(listener);
        return () -> propertyListeners.remove(listener);
    }

    @Override
    protected Serializable put(String key, Serializable value,
            boolean emitChange) {
        PutResult result = putWithDeferredChangeEvent(key, value, emitChange);

        // Fire event right away (if applicable)
        result.run();

        return result.oldValue;
    }

    private class PutResult implements Runnable {
        private final Serializable oldValue;
        private final PropertyChangeEvent eventToFire;

        public PutResult(Serializable oldValue,
                PropertyChangeEvent eventToFire) {
            this.oldValue = oldValue;
            this.eventToFire = eventToFire;
        }

        @Override
        public void run() {
            if (eventToFire != null) {
                fireEvent(eventToFire);
            }
        }
    }

    private PutResult putWithDeferredChangeEvent(String key, Serializable value,
            boolean emitChange) {
        Serializable oldValue = super.put(key, value, emitChange);
        boolean valueChanged = !Objects.equals(oldValue, value);

        PropertyChangeEvent event;
        if (hasElement() && valueChanged) {
            event = new PropertyChangeEvent(Element.get(getNode()), key,
                    oldValue, !emitChange);
        } else {
            event = null;
        }

        return new PutResult(oldValue, event);
    }

    @Override
    protected Serializable remove(String key) {
        Serializable oldValue = super.remove(key);

        fireEvent(new PropertyChangeEvent(Element.get(getNode()), key, oldValue,
                true));

        return oldValue;
    }

    @Override
    protected boolean mayUpdateFromClient(String key, Serializable value) {
        return allowUpdateFromClient(key, value);
    }

    private boolean allowUpdateFromClient(String key, Serializable value) {
        AllowUpdate isAllowed = isUpdateFromClientAllowedBeforeFilter(key);
        if (!AllowUpdate.NO_EXPLICIT_STATUS.equals(isAllowed)) {
            return AllowUpdate.EXPLICITLY_ALLOW.equals(isAllowed);
        }

        isAllowed = isUpdateFromClientAllowedByFilter(getNode(), key, false);
        if (!AllowUpdate.NO_EXPLICIT_STATUS.equals(isAllowed)) {
            return AllowUpdate.EXPLICITLY_ALLOW.equals(isAllowed);
        }
        return false;
    }

    private boolean isDisallowedByFilter(String key) {
        /*
         * Ignore updates for properties that are rejected because of model type
         * definitions. Such changes should preferably not be sent by the client
         * at all, but additional bookkeeping would be needed to allow the
         * client to know which properties are actually allowed.
         */
        if (AllowUpdate.NO_EXPLICIT_STATUS
                .equals(isUpdateFromClientAllowedBeforeFilter(key))) {
            // If we are here it means that either there is no filter or the
            // filter disallows the update
            AllowUpdate allowed = isUpdateFromClientAllowedByFilter(getNode(),
                    key, true);
            if (!AllowUpdate.NO_EXPLICIT_STATUS.equals(allowed)) {
                // This condition means there is a filter which explicitly
                // allows or disallows the property
                assert AllowUpdate.EXPLICITLY_DISALLOW
                        .equals(allowed) : "Implementation error. If update for a property is allowed before the "
                                + "filter it's expected that the filter disallow it";
                return true;
            }
        }
        return false;
    }

    private AllowUpdate isUpdateFromClientAllowedBeforeFilter(String property) {
        if (forbiddenProperties.contains(property)) {
            return AllowUpdate.EXPLICITLY_DISALLOW;
        }
        StateNode node = getNode();

        if (node.hasFeature(SynchronizedPropertiesList.class)
                && node.getFeature(SynchronizedPropertiesList.class)
                        .getSynchronizedProperties().contains(property)) {
            return AllowUpdate.EXPLICITLY_ALLOW;
        } else if (node.hasFeature(ElementListenerMap.class)
                && node.getFeature(ElementListenerMap.class)
                        .getPropertySynchronizationMode(property) != null) {
            return AllowUpdate.EXPLICITLY_ALLOW;
        }
        return AllowUpdate.NO_EXPLICIT_STATUS;
    }

    private AllowUpdate isUpdateFromClientAllowedByFilter(StateNode node,
            String key, boolean log) {
        if (node.hasFeature(ElementPropertyMap.class)) {
            ElementPropertyMap propertyMap = node
                    .getFeature(ElementPropertyMap.class);
            if (propertyMap.updateFromClientFilter != null) {
                boolean allow = propertyMap.updateFromClientFilter.test(key);
                if (!allow && log) {
                    getLogger().warn(
                            "Ignoring model update for {}. "
                                    + "For security reasons, the property must have a "
                                    + "two-way binding in the template, be annotated with @{} in the model, or be defined as synchronized.",
                            key, AllowClientUpdates.class.getSimpleName());
                }
                return allow ? AllowUpdate.EXPLICITLY_ALLOW
                        : AllowUpdate.EXPLICITLY_DISALLOW;
            }
        }
        StateNode parent = node.getParent();
        if (parent == null) {
            return AllowUpdate.NO_EXPLICIT_STATUS;
        }
        if (parent.hasFeature(ElementPropertyMap.class)) {
            ElementPropertyMap parentMap = parent
                    .getFeature(ElementPropertyMap.class);
            Optional<String> parentProperty = parentMap.getPropertyNames()
                    .filter(property -> node.equals(parentMap.get(property)))
                    .findFirst();
            if (parentProperty.isPresent()) {
                String property = new StringBuilder(parentProperty.get())
                        .append('.').append(key).toString();
                return isUpdateFromClientAllowedByFilter(parent, property, log);
            }
        }
        if (parent.hasFeature(ModelList.class)) {
            ModelList list = parent.getFeature(ModelList.class);
            if (list.contains(node)) {
                return isUpdateFromClientAllowedByFilter(parent, key, log);
            }
        }
        return AllowUpdate.NO_EXPLICIT_STATUS;
    }

    /**
     * Sets a filter that will be used by for determining whether a property
     * maybe updated from the client. The filter is recursively inherited to
     * child model properties.
     *
     * @param updateFromClientFilter
     *            the filter to set, or <code>null</code> to remove the current
     *            filter
     */
    public void setUpdateFromClientFilter(
            SerializablePredicate<String> updateFromClientFilter) {
        this.updateFromClientFilter = updateFromClientFilter;
    }

    /**
     * Gets a model map using the given key.
     * <p>
     * If the key is not mapped to a value, creates a model map for the key.
     *
     * @param key
     *            the key to use for the lookup
     * @return a model map attached to the given key, possibly created in this
     *         method
     */
    private ElementPropertyMap getOrCreateModelMap(String key) {
        Serializable value = getProperty(key);
        if (value == null) {
            value = new StateNode(
                    Collections.singletonList(ElementPropertyMap.class));
            setProperty(key, value);
        }

        assert value instanceof StateNode;
        assert ((StateNode) value).hasFeature(ElementPropertyMap.class);
        return ((StateNode) value).getFeature(ElementPropertyMap.class);
    }

    /**
     * Gets a model list using the given key.
     * <p>
     * If the key is not mapped to a value, creates a model list for the key.
     *
     * @param key
     *            the key to use for the lookup
     * @return a model list attached to the given key, possibly created in this
     *         method
     */
    private ModelList getOrCreateModelList(String key) {
        Serializable value = getProperty(key);
        if (value == null) {
            value = new StateNode(Collections.singletonList(ModelList.class));
            setProperty(key, value);
        }

        assert value instanceof StateNode;
        assert ((StateNode) value).hasFeature(ModelList.class);
        return ((StateNode) value).getFeature(ModelList.class);
    }

    /**
     * Resolves the {@link ElementPropertyMap} that the model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @return the resolved model map
     */
    public ElementPropertyMap resolveModelMap(String modelPath) {
        if ("".equals(modelPath)) {
            return this;
        }
        return resolve(modelPath, ElementPropertyMap.class);
    }

    /**
     * Resolves the {@link ModelList} that the model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @return the resolved model list
     */
    public ModelList resolveModelList(String modelPath) {
        return resolve(modelPath, ModelList.class);
    }

    /**
     * Resolves the {@link ModelList} or {@link ElementPropertyMap} that the
     * model path refers to.
     * <p>
     * If the model path contains separate dot separated parts, any non-existing
     * part will be created during resolving.
     *
     * @param modelPath
     *            the path to resolve, either a single property name or a dot
     *            separated path
     * @param leafType
     *            the type of feature to resolve, {@link ModelList} or
     *            {@link ElementPropertyMap}
     * @return the resolved model list or map
     */
    @SuppressWarnings("unchecked")
    private <T extends NodeFeature> T resolve(String modelPath,
            Class<T> leafType) {
        assert modelPath != null;
        assert !"".equals(modelPath);
        assert !modelPath.startsWith(".");
        assert !modelPath.endsWith(".");
        assert leafType == ElementPropertyMap.class
                || leafType == ModelList.class;

        int dotLocation = modelPath.indexOf('.');
        if (dotLocation == -1) {
            if (leafType == ElementPropertyMap.class) {
                return (T) getOrCreateModelMap(modelPath);
            } else {
                return (T) getOrCreateModelList(modelPath);
            }
        } else {
            String firstKey = modelPath.substring(0, dotLocation);
            String remainingPath = modelPath.substring(dotLocation + 1);
            ElementPropertyMap subMap = getOrCreateModelMap(firstKey);
            return subMap.resolve(remainingPath, leafType);
        }
    }

    /**
     * Gets the model map for the given node.
     * <p>
     * Throws an exception if the node does not have a model map.
     *
     * @param node
     *            the node which has a model map
     * @return the model map for the node
     */
    public static ElementPropertyMap getModel(StateNode node) {
        assert node != null;
        return node.getFeature(ElementPropertyMap.class);
    }

    private void fireEvent(PropertyChangeEvent event) {
        if (listeners == null) {
            return;
        }
        List<PropertyChangeListener> propertyListeners = listeners
                .get(event.getPropertyName());
        if (propertyListeners != null && !propertyListeners.isEmpty()) {
            new ArrayList<>(propertyListeners)
                    .forEach(listener -> listener.propertyChange(event));
        }
    }

    private boolean hasElement() {
        return getNode().hasFeature(ElementData.class);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ElementPropertyMap.class);
    }

    /**
     * The method first checks whether the update from client is allowed using
     * the method {@link #allowUpdateFromClient(String, Serializable)}. Then if
     * it's not allowed then it either throws or returns NO OPERATION runnable
     * in case if {@link #updateFromClientFilter} disallows the update (in this
     * case it's just an application business logic and we should not throw).
     *
     * The logic inside the {@link #allowUpdateFromClient(String, Serializable)}
     * check block repeats its own logic to make sure that:
     * <ul>
     * <li>It's in sync with
     * {@link #allowUpdateFromClient(String, Serializable)} (and
     * {@link #mayUpdateFromClient(String, Serializable)}
     * <li>The update is disallowed by the filter (and not some other checks
     * that are inside {@link #allowUpdateFromClient(String, Serializable)}
     * <ul>
     *
     * Here is the logic flow:
     *
     * <pre>



                             +--------------------------------+
                             |                                |
                             | allowUpdateFromClient  is false|
                             |                                |
                             +--------------+-----------------+
                                            |
                                            |
                                            v
                       +-------------------------------------------------+
                       |                                                 |
                       |    isUpdateFromClientAllowedBeforeFilter        |
                       |                                                 |
                       +--+-----------------+-------------------------+--+     +----------------------+
                          |                 |                         |        |                      |
                          |                 |                         |        | NO_EXPLICIT_STATUS   |
    +-----------------+   |                 |                         |        |                      |
    |                 |   |                 v                         +----&gt;   |   The proeprty is    |
    |  DISALLOW       |&lt;--        +----------------------------------+         |not forbidden and     |
    |                 |           |           ALLOW                  |         |it is not synhronized |
    | The property is |           | The property is explicitly       |         |  Check whether       |
    | forbidden and   |           | synchronized and should allow    |         |updateFromClientFilter|
    |  filter is not  |           |       update                     |         | exists and disallows |
    |  involved       |           +----------------------------------+         |   the property update|
    +-----------------+                    |                                   +----------------------+
               |                           |                                     |
               |                           |                                     |
               |                           |                                     |
               |                           |                                     |
               v                           v                                     v
        +-----------+          +-------------------------+     +-----------------------------------+
        |           |          |                         |     |                                   |
        |  throw    |          |  It's no possible since |     |  If there is a filter for the     |
        |           |          |  the property is        |     |  current node or node parent then |
        |           |          |    disallowed           |     |  filter result should be false    |
        +-----------+          +-------------------------+     | (since we already in block where  |
                                                               |   the property is disallowed).    |
                                                               |  Otherwise there is no filter at  |
                                                               |  all and we should throw.         |
                                                               +-+-----------------+---------------+
                                                                 |                 |
                                                                 |                 |
                                                                 |                 |
                               +-----------------------------+   |                 |
                               |   There is a filter         |   |                 |
                               |                             |&lt;--+                 v
                               | Return no op runnable       |         +-------------------------------+
                               |                             |         |    There is no a filter       |
                               +-----------------------------+         |                               |
                                                                       |      throw                    |
                                                                       +-------------------------------+
     *
     * </pre>
     */
    private Runnable doDeferredUpdateFromClient(String key,
            Serializable value) {
        // Use private <code>allowUpdateFromClient</code> method instead of
        // <code>mayUpdateFromClient</code> which may be overridden
        // The logic below
        if (!allowUpdateFromClient(key, value)) {
            if (isDisallowedByFilter(key)) {
                return () -> {
                    // nop
                };
            }

            throw new IllegalArgumentException(String.format(
                    "Feature '%s' doesn't allow the client to update '%s'. "
                            + "For security reasons, the property must be defined as synchronized through the Element's API.",
                    getClass().getName(), key));

        }

        return putWithDeferredChangeEvent(key, value, false);
    }
}
