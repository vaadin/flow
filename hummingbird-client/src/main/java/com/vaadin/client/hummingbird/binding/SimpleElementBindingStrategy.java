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
package com.vaadin.client.hummingbird.binding;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;
import com.vaadin.client.hummingbird.collection.JsSet;
import com.vaadin.client.hummingbird.nodefeature.ListSpliceEvent;
import com.vaadin.client.hummingbird.nodefeature.MapProperty;
import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.client.hummingbird.nodefeature.NodeMap;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.Reactive;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration;
import elemental.dom.DOMTokenList;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import jsinterop.annotations.JsFunction;

/**
 * Binding strategy for a simple (not template) {@link Element} node.
 * 
 * @author Vaadin Ltd
 *
 */
public class SimpleElementBindingStrategy implements BindingStrategy<Element> {

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

    /**
     * Just a context class whose instance is passed as a parameter between the
     * operations of various kind to be able to access the data like listeners,
     * node and element which they operate on.
     * <p>
     * It's used to avoid having methods with a long numbers of parameters and
     * because the strategy instance is stateless.
     *
     */
    private static class BindingContext {

        private final Element element;
        private final StateNode node;
        private final BinderContext binderContext;

        private final JsMap<String, Computation> listenerBindings = JsCollections
                .map();
        private final JsMap<String, EventRemover> listenerRemovers = JsCollections
                .map();

        private final JsSet<EventRemover> synchronizedPropertyEventListeners = JsCollections
                .set();

        private BindingContext(StateNode node, Element element,
                BinderContext binderContext) {
            this.node = node;
            this.element = element;
            this.binderContext = binderContext;
        }
    }

    @Override
    public Element create(StateNode node) {
        String tag = getTag(node);

        assert tag != null : "New child must have a tag";

        return Browser.getDocument().createElement(tag);
    }

    @Override
    public boolean isAppliable(StateNode node) {
        if (node.hasFeature(NodeFeatures.ELEMENT_DATA)
                || node.hasFeature(NodeFeatures.OVERRIDE_DATA)) {
            return true;
        }
        Optional<StateNode> root = Optional.of(node).map(StateNode::getTree)
                .map(StateTree::getRootNode);
        return root.isPresent() && root.get() == node;
    }

    @Override
    public void bind(StateNode stateNode, Element htmlNode,
            BinderContext nodeFactory) {
        assert hasSameTag(stateNode, htmlNode);

        BindingContext context = new BindingContext(stateNode, htmlNode,
                nodeFactory);

        JsArray<JsMap<String, Computation>> computationsCollection = JsCollections
                .array();

        JsArray<EventRemover> listeners = JsCollections.array();

        listeners.push(bindMap(NodeFeatures.ELEMENT_PROPERTIES,
                property -> updateProperty(property, htmlNode),
                createComputations(computationsCollection), stateNode));
        listeners.push(bindMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES,
                property -> updateStyleProperty(property, htmlNode),
                createComputations(computationsCollection), stateNode));
        listeners.push(bindMap(NodeFeatures.ELEMENT_ATTRIBUTES,
                property -> updateAttribute(property, htmlNode),
                createComputations(computationsCollection), stateNode));

        listeners.push(bindSynchronizedPropertyEvents(context));

        listeners.push(bindChildren(context));

        listeners.push(stateNode.addUnregisterListener(
                e -> remove(listeners, context, computationsCollection)));

        listeners.push(bindDomEventListeners(context));

        listeners.push(bindClassList(htmlNode, stateNode));
    }

    @SuppressWarnings("unchecked")
    private JsMap<String, Computation> createComputations(
            JsArray<JsMap<String, Computation>> computationsCollection) {
        JsMap<String, Computation> computations = JsCollections.map();
        computationsCollection.push(computations);
        return computations;
    }

    private boolean hasSameTag(StateNode node, Element element) {
        String nsTag = getTag(node);
        return nsTag == null || element.getTagName().equalsIgnoreCase(nsTag);
    }

    private EventRemover bindMap(int featureId, PropertyUser user,
            JsMap<String, Computation> bindings, StateNode node) {
        NodeMap map = node.getMap(featureId);
        map.forEachProperty(
                (property, name) -> bindProperty(user, property, bindings));

        return map.addPropertyAddListener(
                e -> bindProperty(user, e.getProperty(), bindings));
    }

    private static void bindProperty(PropertyUser user, MapProperty property,
            JsMap<String, Computation> bindings) {
        String name = property.getName();

        assert !bindings.has(name) : "There's already a binding for " + name;

        Computation computation = Reactive
                .runWhenDepedenciesChange(() -> user.use(property));

        bindings.set(name, computation);
    }

    private void updateProperty(MapProperty mapProperty, Element element) {
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

    private void updateStyleProperty(MapProperty mapProperty, Element element) {
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

    private void updateAttribute(MapProperty mapProperty, Element element) {
        String name = mapProperty.getName();

        if (mapProperty.hasValue()) {
            element.setAttribute(name, String.valueOf(mapProperty.getValue()));
        } else {
            element.removeAttribute(name);
        }
    }

    private EventRemover bindSynchronizedPropertyEvents(
            BindingContext context) {
        synchronizeEventTypesChanged(context);

        NodeList propertyEvents = context.node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);
        return propertyEvents
                .addSpliceListener(e -> synchronizeEventTypesChanged(context));
    }

    private void synchronizeEventTypesChanged(BindingContext context) {
        NodeList propertyEvents = context.node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        // Remove all old listeners and add new ones
        context.synchronizedPropertyEventListeners
                .forEach(EventRemover::remove);
        context.synchronizedPropertyEventListeners.clear();

        for (int i = 0; i < propertyEvents.length(); i++) {
            String eventType = propertyEvents.get(i).toString();
            EventRemover remover = context.element.addEventListener(eventType,
                    event -> handlePropertySyncDomEvent(context), false);
            context.synchronizedPropertyEventListeners.add(remover);
        }
    }

    private void handlePropertySyncDomEvent(BindingContext context) {
        NodeList propertiesList = context.node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);
        for (int i = 0; i < propertiesList.length(); i++) {
            syncPropertyIfNeeded(propertiesList.get(i).toString(), context);
        }
    }

    /**
     * Synchronizes the given property if the value in the DOM does not match
     * the value in the StateTree.
     * <p>
     * Updates the StateTree with the new property value as a side effect.
     *
     * @param propertyName
     *            the name of the property
     * @param context
     *            operation context
     */
    private void syncPropertyIfNeeded(String propertyName,
            BindingContext context) {
        Object currentValue = WidgetUtil.getJsProperty(context.element,
                propertyName);

        // Server side value from tree
        Object treeValue = null;

        MapProperty treeProperty = context.node
                .getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty(propertyName);
        if (treeProperty.hasValue()) {
            treeValue = treeProperty.getValue();
        }

        if (!Objects.equals(currentValue, treeValue)) {
            context.node.getTree().sendPropertySyncToServer(context.node,
                    propertyName, currentValue);
            // Update tree so we don't send this again and again.
            treeProperty.setValue(currentValue);
        }

    }

    private EventRemover bindChildren(BindingContext context) {
        return context.binderContext
                .populateChildren(context.element, context.node,
                        NodeFeatures.ELEMENT_CHILDREN,
                        context.binderContext::createAndBind)
                .addSpliceListener(e -> {
                    /*
                     * Handle lazily so we can create the children we need to
                     * insert. The change that gives a child node an element tag
                     * name might not yet have been applied at this point.
                     */
                    Reactive.addFlushListener(
                            () -> handleChildrenSplice(e, context));
                });
    }

    private void handleChildrenSplice(ListSpliceEvent event,
            BindingContext context) {
        JsArray<?> remove = event.getRemove();
        for (int i = 0; i < remove.length(); i++) {
            StateNode childNode = (StateNode) remove.get(i);
            Node child = childNode.getDomNode();

            assert child != null : "Can't find element to remove";

            assert child
                    .getParentElement() == context.element : "Invalid element parent";

            context.element.removeChild(child);
        }

        JsArray<?> add = event.getAdd();
        if (add.length() != 0) {
            int insertIndex = event.getIndex();
            elemental.dom.NodeList childNodes = context.element.getChildNodes();

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
                Node childNode = context.binderContext
                        .createAndBind((StateNode) newChildObject);

                context.element.insertBefore(childNode, beforeRef);

                beforeRef = childNode.getNextSibling();
            }
        }
    }

    /**
     * Removes all bindings.
     */
    private void remove(JsArray<EventRemover> listeners, BindingContext context,
            JsArray<JsMap<String, Computation>> computationsCollection) {
        ForEachCallback<String, Computation> computationStopper = (computation,
                name) -> computation.stop();

        computationsCollection
                .forEach(collection -> collection.forEach(computationStopper));
        context.listenerBindings.forEach(computationStopper);

        context.listenerRemovers.forEach((remover, name) -> remover.remove());
        listeners.forEach(EventRemover::remove);
        context.synchronizedPropertyEventListeners
                .forEach(EventRemover::remove);
    }

    private EventRemover bindDomEventListeners(BindingContext context) {
        NodeMap elementListeners = getDomEventListenerMap(context.node);
        elementListeners.forEachProperty((property,
                name) -> bindEventHandlerProperty(property, context));

        return elementListeners.addPropertyAddListener(
                event -> bindEventHandlerProperty(event.getProperty(),
                        context));
    }

    private void bindEventHandlerProperty(MapProperty eventHandlerProperty,
            BindingContext context) {
        String name = eventHandlerProperty.getName();
        assert !context.listenerBindings.has(name);

        Computation computation = Reactive.runWhenDepedenciesChange(() -> {
            boolean hasValue = eventHandlerProperty.hasValue();
            boolean hasListener = context.listenerRemovers.has(name);

            if (hasValue != hasListener) {
                if (hasValue) {
                    addEventHandler(name, context);
                } else {
                    removeEventHandler(name, context);
                }
            }
        });

        context.listenerBindings.set(name, computation);

    }

    private void removeEventHandler(String eventType, BindingContext context) {
        EventRemover remover = context.listenerRemovers.get(eventType);
        context.listenerRemovers.delete(eventType);

        assert remover != null;
        remover.remove();
    }

    private void addEventHandler(String eventType, BindingContext context) {
        assert !context.listenerRemovers.has(eventType);

        EventRemover remover = context.element.addEventListener(eventType,
                event -> handleDomEvent(event, context.element, context.node),
                false);

        context.listenerRemovers.set(eventType, remover);
    }

    private NodeMap getDomEventListenerMap(StateNode node) {
        return node.getMap(NodeFeatures.ELEMENT_LISTENERS);
    }

    private void handleDomEvent(Event event, Element element, StateNode node) {
        String type = event.getType();

        NodeMap listenerMap = getDomEventListenerMap(node);

        @SuppressWarnings("unchecked")
        JsArray<String> dataExpressions = (JsArray<String>) listenerMap
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

    private EventRemover bindClassList(Element element, StateNode node) {
        NodeList classNodeList = node.getList(NodeFeatures.CLASS_LIST);

        for (int i = 0; i < classNodeList.length(); i++) {
            element.getClassList().add((String) classNodeList.get(i));
        }

        return classNodeList.addSpliceListener(e -> {
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
}
