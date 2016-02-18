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

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;
import com.vaadin.client.hummingbird.namespace.ListNamespace;
import com.vaadin.client.hummingbird.namespace.ListSpliceEvent;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.client.hummingbird.namespace.MapProperty;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.NodeList;
import elemental.events.EventRemover;

/**
 * Binds element related state node namespaces to an element instance.
 *
 * @since
 * @author Vaadin Ltd
 */
public class BasicElementBinder {
    @FunctionalInterface
    private interface PropertyUser {
        public void use(MapProperty property);
    }

    private final JsMap<String, Computation> propertyBindings = JsCollections
            .map();
    private final JsMap<String, Computation> attributeBindings = JsCollections
            .map();
    private final JsMap<String, Computation> listenerBindings = JsCollections
            .map();
    private final JsMap<String, EventRemover> listenerRemovers = JsCollections
            .map();

    private final EventRemover unregisterListener;
    private final EventRemover listenersListener;
    private final EventRemover childrenListener;

    private final Element element;
    private final StateNode node;

    private BasicElementBinder(StateNode node, Element element) {
        assert node.getElement() == null;

        this.node = node;
        this.element = element;

        String nsTag = getTag(node);
        assert nsTag == null
                || element.getTagName().toLowerCase().equals(nsTag);

        bindMap(Namespaces.ELEMENT_PROPERTIES, propertyBindings,
                this::updateProperty);

        bindMap(Namespaces.ELEMENT_ATTRIBUTES, attributeBindings,
                this::updateAttribute);

        childrenListener = bindChildren();

        unregisterListener = node.addUnregisterListener(e -> remove());

        listenersListener = bindListeners();

        node.setElement(element);
    }

    private static String getTag(StateNode node) {
        return (String) node.getMapNamespace(Namespaces.ELEMENT_DATA)
                .getProperty(Namespaces.TAG).getValue();
    }

    private EventRemover bindListeners() {
        MapNamespace listeners = node
                .getMapNamespace(Namespaces.ELEMENT_LISTENERS);
        listeners.forEachProperty(
                (property, name) -> bindEventHandlerProperty(property));

        return listeners.addPropertyAddListener(
                event -> bindEventHandlerProperty(event.getProperty()));
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
                e -> node.getTree().sendEventToServer(node, e), false);

        listenerRemovers.set(eventType, remover);
    }

    private void removeEventHandler(String eventType) {
        EventRemover remover = listenerRemovers.get(eventType);
        listenerRemovers.delete(eventType);

        assert remover != null;
        remover.remove();
    }

    private void bindMap(int namespaceId, JsMap<String, Computation> bindings,
            PropertyUser user) {
        MapNamespace namespace = node.getMapNamespace(namespaceId);
        namespace.forEachProperty(
                (property, name) -> bindProperty(bindings, user, property));

        namespace.addPropertyAddListener(
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
            WidgetUtil.setJsProperty(element, name, mapProperty.getValue());
        } else if (WidgetUtil.hasOwnJsProperty(element, name)) {
            WidgetUtil.deleteJsProperty(element, name);
        } else {
            // Can't delete inherited property, so instead just clear
            // the value
            WidgetUtil.setJsProperty(element, name, null);
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
            Element child = childNode.getElement();

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

        propertyBindings.forEach(computationStopper);
        attributeBindings.forEach(computationStopper);
        listenerBindings.forEach(computationStopper);

        listenerRemovers.forEach((remover, name) -> remover.remove());

        listenersListener.remove();
        unregisterListener.remove();
        childrenListener.remove();
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
