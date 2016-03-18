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

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;
import com.vaadin.client.hummingbird.collection.JsSet;
import com.vaadin.client.hummingbird.namespace.ListNamespace;
import com.vaadin.client.hummingbird.namespace.ListSpliceEvent;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.namespace.SynchronizedPropertiesNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration;
import elemental.dom.DOMTokenList;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.NodeList;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import jsinterop.annotations.JsFunction;

/**
 * Binds element related state node namespaces to an element instance.
 *
 * @since
 * @author Vaadin Ltd
 */
public class BasicElementBinder {
    @FunctionalInterface
    private interface PropertyUser {
        void use(MapProperty property);
    }

    /**
     * Callback interface for an event data expression parsed using new
     * Function() in JavaScript.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    private interface EventDataExpression {
        JsonValue evaluate(Event event, Element element);
    }

    private static final JsMap<String, EventDataExpression> expressionCache = JsCollections
            .map();

    private final JsMap<String, Computation> propertyBindings = JsCollections
            .map();
    private final JsMap<String, Computation> stylePropertyBindings = JsCollections
            .map();
    private final JsMap<String, Computation> attributeBindings = JsCollections
            .map();
    private final JsMap<String, Computation> listenerBindings = JsCollections
            .map();
    private final JsMap<String, EventRemover> listenerRemovers = JsCollections
            .map();
    private Computation synchronizedPropertyComputation;

    private final JsArray<EventRemover> listeners = JsCollections.array();
    private final JsSet<EventRemover> synchronizedPropertyEventListeners = JsCollections
            .set();

    private final Element element;
    private final StateNode node;

    private BasicElementBinder(StateNode node, Element element) {
        assert node.getDomNode() == null;

        this.node = node;
        this.element = element;

        String nsTag = getTag(node);
        assert nsTag == null
                || element.getTagName().toLowerCase().equals(nsTag);

        listeners.push(bindMap(Namespaces.ELEMENT_PROPERTIES, propertyBindings,
                this::updateProperty));
        listeners.push(bindMap(Namespaces.ELEMENT_STYLE_PROPERTIES,
                stylePropertyBindings, this::updateStyleProperty));
        listeners.push(bindMap(Namespaces.ELEMENT_ATTRIBUTES, attributeBindings,
                this::updateAttribute));

        bindSynchronizedProperties();

        listeners.push(bindChildren());

        listeners.push(node.addUnregisterListener(e -> remove()));

        listeners.push(bindDomEventListeners());

        listeners.push(bindClassList());

        node.setDomNode(element);
    }

    private void bindSynchronizedProperties() {
        MapProperty eventTypesProperty = node
                .getMapNamespace(Namespaces.SYNCHRONIZED_PROPERTIES)
                .getProperty(SynchronizedPropertiesNamespace.KEY_EVENTS);
        synchronizedPropertyComputation = Reactive.runWhenDepedenciesChange(
                () -> synchronizeEventTypesChanged(eventTypesProperty));
    }

    @SuppressWarnings("unchecked")
    private void synchronizeEventTypesChanged(MapProperty eventTypesProperty) {
        // Remove all old listeners and add new ones
        synchronizedPropertyEventListeners.forEach(EventRemover::remove);
        synchronizedPropertyEventListeners.clear();

        if (eventTypesProperty.hasValue()) {
            JsArray<String> syncEvents = (JsArray<String>) eventTypesProperty
                    .getValue();

            syncEvents.forEach(eventType -> {
                EventRemover remover = element.addEventListener(eventType,
                        this::handlePropertySyncDomEvent, false);
                synchronizedPropertyEventListeners.add(remover);
            });
        }

    }

    private EventRemover bindClassList() {
        ListNamespace namespace = node.getListNamespace(Namespaces.CLASS_LIST);

        for (int i = 0; i < namespace.length(); i++) {
            element.getClassList().add((String) namespace.get(i));
        }

        return namespace.addSpliceListener(e -> {
            DOMTokenList classList = element.getClassList();

            JsArray<?> remove = e.getRemove();
            for (int i = 0; i < remove.length(); i++) {
                classList.remove((String) remove.get(i));
            }

            JsArray<?> add = e.getAdd();
            for (int i = 0; i < add.length(); i++) {
                classList.add((String) add.get(i));
            }
        });
    }

    private static String getTag(StateNode node) {
        return (String) node.getMapNamespace(Namespaces.ELEMENT_DATA)
                .getProperty(Namespaces.TAG).getValue();
    }

    private EventRemover bindDomEventListeners() {
        MapNamespace elementListeners = getDomEventListenerNamespace();
        elementListeners.forEachProperty(
                (property, name) -> bindEventHandlerProperty(property));

        return elementListeners.addPropertyAddListener(
                event -> bindEventHandlerProperty(event.getProperty()));
    }

    private MapNamespace getDomEventListenerNamespace() {
        return node.getMapNamespace(Namespaces.ELEMENT_LISTENERS);
    }

    private void bindEventHandlerProperty(MapProperty eventHandlerProperty) {
        String name = eventHandlerProperty.getName();
        assert !listenerBindings.has(name);

        Computation computation = Reactive.runWhenDepedenciesChange(() -> {
            boolean hasValue = eventHandlerProperty.hasValue();
            boolean hasListener = listenerRemovers.has(name);

            if (hasValue != hasListener) {
                if (hasValue) {
                    addEventHandler(name);
                } else {
                    removeEventHandler(name);
                }
            }
        });

        listenerBindings.set(name, computation);

    }

    private void addEventHandler(String eventType) {
        assert !listenerRemovers.has(eventType);

        EventRemover remover = element.addEventListener(eventType,
                this::handleDomEvent, false);

        listenerRemovers.set(eventType, remover);
    }

    private void handleDomEvent(Event event) {
        String type = event.getType();

        MapNamespace listenersNamespace = getDomEventListenerNamespace();

        @SuppressWarnings("unchecked")
        JsArray<String> dataExpressions = (JsArray<String>) listenersNamespace
                .getProperty(type).getValue();

        JsonObject eventData;
        if (dataExpressions == null || dataExpressions.isEmpty()) {
            eventData = null;
        } else {
            eventData = Json.createObject();

            for (int i = 0; i < dataExpressions.length(); i++) {
                String expressionString = dataExpressions.get(i);

                EventDataExpression expression = getOrCreateExpression(
                        expressionString);

                JsonValue expressionValue = expression.evaluate(event, element);

                eventData.put(expressionString, expressionValue);
            }
        }

        node.getTree().sendEventToServer(node, type, eventData);
    }

    private static EventDataExpression getOrCreateExpression(
            String expressionString) {
        EventDataExpression expression = expressionCache.get(expressionString);

        if (expression == null) {
            expression = NativeFunction.create("event", "element",
                    "return (" + expressionString + ")");
            expressionCache.set(expressionString, expression);
        }

        return expression;
    }

    private void removeEventHandler(String eventType) {
        EventRemover remover = listenerRemovers.get(eventType);
        listenerRemovers.delete(eventType);

        assert remover != null;
        remover.remove();
    }

    private EventRemover bindMap(int namespaceId,
            JsMap<String, Computation> bindings, PropertyUser user) {
        MapNamespace namespace = node.getMapNamespace(namespaceId);
        namespace.forEachProperty(
                (property, name) -> bindProperty(bindings, user, property));

        return namespace.addPropertyAddListener(
                e -> bindProperty(bindings, user, e.getProperty()));
    }

    private static void bindProperty(JsMap<String, Computation> bindings,
            PropertyUser user, MapProperty property) {
        String name = property.getName();

        assert !bindings.has(name) : "There's already a binding for " + name;

        Computation computation = Reactive
                .runWhenDepedenciesChange(() -> user.use(property));

        bindings.set(name, computation);

    }

    private void updateProperty(MapProperty mapProperty) {
        String name = mapProperty.getName();
        if (mapProperty.hasValue()) {
            Object treeValue = mapProperty.getValue();
            Object domValue = WidgetUtil.getJsProperty(element, name);
            // We compare with the current property to avoid setting properties
            // which are updated on the client side, e.g. when synchronizing
            // properties to the server (won't work for readonly properties).
            if (!Objects.equals(domValue, treeValue)) {
                WidgetUtil.setJsProperty(element, name, treeValue);
            }
        } else if (WidgetUtil.hasOwnJsProperty(element, name)) {
            WidgetUtil.deleteJsProperty(element, name);
        } else {
            // Can't delete inherited property, so instead just clear
            // the value
            WidgetUtil.setJsProperty(element, name, null);
        }
    }

    private void updateStyleProperty(MapProperty mapProperty) {
        String name = mapProperty.getName();
        CSSStyleDeclaration styleElement = element.getStyle();
        if (mapProperty.hasValue()) {
            WidgetUtil.setJsProperty(styleElement, name,
                    mapProperty.getValue());
        } else {
            // Can't delete a style property, so just clear the value
            WidgetUtil.setJsProperty(styleElement, name, null);
        }
    }

    private void updateAttribute(MapProperty mapProperty) {
        String name = mapProperty.getName();

        if (mapProperty.hasValue()) {
            element.setAttribute(name, String.valueOf(mapProperty.getValue()));
        } else {
            element.removeAttribute(name);
        }
    }

    @SuppressWarnings("unchecked")
    private JsArray<String> getPropertiesToSync() {
        MapNamespace namespace = node
                .getMapNamespace(Namespaces.SYNCHRONIZED_PROPERTIES);
        MapProperty p = namespace
                .getProperty(SynchronizedPropertiesNamespace.KEY_PROPERTIES);
        if (!p.hasValue()) {
            // No properties to sync
            return JsCollections.array();
        }

        return (JsArray<String>) p.getValue();
    }

    private void handlePropertySyncDomEvent(Event event) {
        getPropertiesToSync()
                .forEach(propertyName -> syncPropertyIfNeeded(propertyName));
    }

    /**
     * Synchronizes the given property if the value in the DOM does not match
     * the value in the StateTree.
     * <p>
     * Updates the StateTree with the new property value as a side effect.
     *
     * @param propertyName
     *            the name of the property
     */
    private void syncPropertyIfNeeded(String propertyName) {
        Object currentValue = WidgetUtil.getJsProperty(element, propertyName);

        // Server side value from tree
        Object treeValue = null;

        MapProperty treeProperty = node
                .getMapNamespace(Namespaces.ELEMENT_PROPERTIES)
                .getProperty(propertyName);
        if (treeProperty.hasValue()) {
            treeValue = treeProperty.getValue();
        }

        if (!Objects.equals(currentValue, treeValue)) {
            node.getTree().sendPropertySyncToServer(node, propertyName,
                    currentValue);
            // Update tree so we don't send this again and again.
            treeProperty.setValue(currentValue);
        }

    }

    private EventRemover bindChildren() {
        ListNamespace children = node
                .getListNamespace(Namespaces.ELEMENT_CHILDREN);

        for (int i = 0; i < children.length(); i++) {
            StateNode childNode = (StateNode) children.get(i);

            Node child = createDomNode(childNode);

            element.appendChild(child);
        }

        return children.addSpliceListener(e -> {
            /*
             * Handle lazily so we can create the children we need to insert.
             * The change that gives a child node an element tag name might not
             * yet have been applied at this point.
             */
            Reactive.addFlushListener(() -> handleChildrenSplice(e));
        });
    }

    private void handleChildrenSplice(ListSpliceEvent event) {
        JsArray<?> remove = event.getRemove();
        for (int i = 0; i < remove.length(); i++) {
            StateNode childNode = (StateNode) remove.get(i);
            Node child = childNode.getDomNode();

            assert child != null : "Can't find element to remove";

            assert child
                    .getParentElement() == element : "Invalid element parent";

            element.removeChild(child);
        }

        JsArray<?> add = event.getAdd();
        if (add.length() != 0) {
            int insertIndex = event.getIndex();
            NodeList childNodes = element.getChildNodes();

            Node beforeRef;
            if (insertIndex < childNodes.length()) {
                // Insert before the node current at the target index
                beforeRef = childNodes.item(insertIndex);
            } else {
                // Insert at the end
                beforeRef = null;
            }

            for (int i = 0; i < add.length(); i++) {
                Object newChildObject = add.get(i);
                Node childNode = createDomNode((StateNode) newChildObject);

                element.insertBefore(childNode, beforeRef);

                beforeRef = childNode.getNextSibling();
            }
        }
    }

    private static Node createDomNode(StateNode node) {
        if (node.hasNamespace(Namespaces.TEXT_NODE)) {
            return TextElementBinder.createAndBind(node);
        }

        String tag = getTag(node);

        assert tag != null : "New child must have a tag";
        // ...or a template, but that's not yet implemented

        Element childElement = Browser.getDocument().createElement(tag);

        BasicElementBinder.bind(node, childElement);

        return childElement;
    }

    /**
     * Removes all bindings.
     */
    public final void remove() {
        ForEachCallback<String, Computation> computationStopper = (computation,
                name) -> computation.stop();

        stylePropertyBindings.forEach(computationStopper);
        propertyBindings.forEach(computationStopper);
        attributeBindings.forEach(computationStopper);
        listenerBindings.forEach(computationStopper);
        synchronizedPropertyComputation.stop();

        listenerRemovers.forEach((remover, name) -> remover.remove());
        listeners.forEach(EventRemover::remove);
        synchronizedPropertyEventListeners.forEach(EventRemover::remove);
    }

    /**
     * Binds a state node to an element.
     *
     * @param node
     *            the state node to bind
     * @param element
     *            the element to bind to
     *
     * @return a basic element binder
     */
    public static BasicElementBinder bind(StateNode node, Element element) {
        return new BasicElementBinder(node, element);
    }
}
