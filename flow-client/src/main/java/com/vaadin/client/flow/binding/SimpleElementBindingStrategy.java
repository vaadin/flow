/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow.binding;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.Command;
import com.vaadin.client.Console;
import com.vaadin.client.PolymerUtils;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.ConstantPool;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsMap.ForEachCallback;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.collection.JsWeakMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.dom.DomElement.DomTokenList;
import com.vaadin.client.flow.nodefeature.ListSpliceEvent;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.nodefeature.NodeFeatures;
import com.vaadin.flow.nodefeature.NodeProperties;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.ShadowRoot;
import elemental.events.Event;
import elemental.events.EventRemover;
import elemental.json.Json;
import elemental.json.JsonArray;
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
        /**
         * Callback interface for an event data expression parsed using new
         * Function() in JavaScript.
         *
         * @param event
         *            Event to expand
         * @param element
         *            target Element
         * @return Result of evaluated function
         */
        JsonValue evaluate(Event event, Element element);
    }

    private static final JsMap<String, EventDataExpression> expressionCache = JsCollections
            .map();

    /**
     * This is used as a weak set. Only keys are important so that they are
     * weakly referenced
     */
    private static final JsWeakMap<StateNode, Boolean> BOUND = JsCollections
            .weakMap();

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

        private final Node htmlNode;
        private final StateNode node;
        private final BinderContext binderContext;

        private final JsMap<String, Computation> listenerBindings = JsCollections
                .map();
        private final JsMap<String, EventRemover> listenerRemovers = JsCollections
                .map();

        private final JsSet<EventRemover> synchronizedPropertyEventListeners = JsCollections
                .set();

        private BindingContext(StateNode node, Node htmlNode,
                BinderContext binderContext) {
            this.node = node;
            this.htmlNode = htmlNode;
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
    public boolean isApplicable(StateNode node) {
        if (node.hasFeature(NodeFeatures.ELEMENT_DATA)
                || node.hasFeature(NodeFeatures.OVERRIDE_DATA)) {
            return true;
        }
        return node.getTree() != null
                && node.equals(node.getTree().getRootNode());
    }

    @Override
    public void bind(StateNode stateNode, Element htmlNode,
            BinderContext nodeFactory) {
        assert hasSameTag(stateNode, htmlNode);

        if (BOUND.has(stateNode)) {
            return;
        }
        BOUND.set(stateNode, true);

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

        listeners.push(bindVirtualChildren(context));

        listeners.push(bindChildren(context));

        listeners.push(stateNode.addUnregisterListener(
                e -> remove(listeners, context, computationsCollection)));

        listeners.push(bindDomEventListeners(context));

        listeners.push(bindClassList(htmlNode, stateNode));

        listeners.push(bindClientDelegateMethods(context));

        listeners.push(bindPolymerEventHandlerNames(context));

        listeners.push(bindShadowRoot(context));

        bindPolymerModelProperties(stateNode, htmlNode);
    }

    private native void bindPolymerModelProperties(StateNode node,
            Element element)
    /*-{
      if ( @com.vaadin.client.PolymerUtils::isPolymerElement(*)(element) ) {
          this.@SimpleElementBindingStrategy::bindPolymerProperties(*)(node, element);
      } else if ( @com.vaadin.client.PolymerUtils::mayBePolymerElement(*)(element) ) {
          var self = this;
          try {
              $wnd.customElements.whenDefined(element.localName).then( function () {
                  self.@SimpleElementBindingStrategy::bindPolymerProperties(*)(node, element);
              });
          }
          catch (e) {
              // ignore the exception: the element cannot be a custom element
          }
      }
    }-*/;

    private native void bindPolymerProperties(StateNode node, Element element)
    /*-{
        this.@SimpleElementBindingStrategy::bindInitialModelProperties(*)(node, element);
        var self = this;

        var originalFunction = element._propertiesChanged;
        if (originalFunction) {
            element._propertiesChanged = function (currentProps, changedProps, oldProps) {
                $entry(function () {
                    self.@SimpleElementBindingStrategy::handlePropertiesChanged(*)(changedProps, node);
                })();
                originalFunction.apply(this, arguments);
            };
        }
    }-*/;

    private void handlePropertiesChanged(
            JavaScriptObject changedPropertyPathsToValues, StateNode node) {
        String[] keys = WidgetUtil.getKeys(changedPropertyPathsToValues);
        for (String propertyName : keys) {
            handlePropertyChange(propertyName, () -> WidgetUtil
                    .getJsProperty(changedPropertyPathsToValues, propertyName),
                    node);
        }
    }

    private void handlePropertyChange(String property,
            Supplier<Object> valueProvider, StateNode node) {
        // This is not the property value itself, its a parent node of the
        // property
        String[] properties = property.split("\\.");
        StateNode model = node;
        MapProperty mapProperty = null;
        for (String prop : properties) {
            NodeMap map = model.getMap(NodeFeatures.ELEMENT_PROPERTIES);
            if (!map.hasPropertyValue(prop)) {
                Console.debug("Ignoring property change for property '"
                        + property + "' which isn't defined from the server");
                /*
                 * Ignore instead of throwing since this is also invoked for
                 * third party polymer components that don't need to have
                 * property changes sent to the server.
                 */
                return;
            }
            mapProperty = map.getProperty(prop);
            if (mapProperty.getValue() instanceof StateNode) {
                model = (StateNode) mapProperty.getValue();
            }
        }
        if (mapProperty.getValue() instanceof StateNode) {
            // Don't send to the server updates for list nodes
            StateNode nodeValue = (StateNode) mapProperty.getValue();
            JsonObject obj = WidgetUtil.crazyJsCast(valueProvider.get());
            if (!obj.hasKey("nodeId")
                    || nodeValue.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
                return;
            }
        }

        mapProperty.syncToServer(valueProvider.get());
    }

    private EventRemover bindShadowRoot(BindingContext context) {
        assert context.htmlNode instanceof Element : "Cannot bind shadow root to a Node";
        NodeMap map = context.node.getMap(NodeFeatures.SHADOW_ROOT_DATA);

        attachShadow(context);

        return map.addPropertyAddListener(event -> Reactive
                .addFlushListener(() -> attachShadow(context)));
    }

    private void attachShadow(BindingContext context) {
        NodeMap map = context.node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
        StateNode shadowRootNode = (StateNode) map
                .getProperty(NodeProperties.SHADOW_ROOT).getValue();
        if (shadowRootNode != null) {
            NativeFunction function = NativeFunction.create("element",
                    "if ( element.shadowRoot ) { return element.shadowRoot; } "
                            + "else { return element.attachShadow({'mode' : 'open'});}");
            Node shadowRoot = (Node) function.call(null, context.htmlNode);

            if (shadowRootNode.getDomNode() == null) {
                shadowRootNode.setDomNode(shadowRoot);
            }

            BindingContext newContext = new BindingContext(shadowRootNode,
                    shadowRoot, context.binderContext);
            bindChildren(newContext);
        }
    }

    private void bindInitialModelProperties(StateNode stateNode,
            Element htmlNode) {
        bindModelProperties(stateNode, htmlNode, "");
    }

    private void bindModelProperties(StateNode stateNode, Element htmlNode,
            String path) {
        Command command = () -> stateNode
                .getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .forEachProperty((property, key) -> bindSubProperty(stateNode,
                        htmlNode, path, property));
        invokeWhenNodeIsConstructed(command, stateNode);
    }

    private void bindSubProperty(StateNode stateNode, Element htmlNode,
            String path, MapProperty property) {
        setSubProperties(htmlNode, property, path);
        PolymerUtils.storeNodeId(htmlNode, stateNode.getId(), path);
    }

    private void setSubProperties(Element htmlNode, MapProperty property,
            String path) {
        String newPath = path.isEmpty() ? property.getName()
                : path + "." + property.getName();
        NativeFunction setValueFunction = NativeFunction.create("path", "value",
                "this.set(path, value)");
        if (property.getValue() instanceof StateNode) {
            StateNode subNode = (StateNode) property.getValue();

            if (subNode.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
                setValueFunction.call(htmlNode, newPath,
                        PolymerUtils.convertToJson(subNode));
                addModelListChangeListener(htmlNode,
                        subNode.getList(NodeFeatures.TEMPLATE_MODELLIST),
                        newPath);
            } else {
                NativeFunction function = NativeFunction.create("path", "value",
                        "this.set(path, {})");
                function.call(htmlNode, newPath);
                bindModelProperties(subNode, htmlNode, newPath);
            }
        } else {
            setValueFunction.call(htmlNode, newPath, property.getValue());
        }
    }

    private void addModelListChangeListener(Element htmlNode,
            NodeList modelList, String polymerModelPath) {
        modelList.addSpliceListener(event -> Reactive
                .addFlushListener(() -> processModelListChange(htmlNode,
                        polymerModelPath, event)));
    }

    private void processModelListChange(Element htmlNode,
            String polymerModelPath, ListSpliceEvent event) {
        JsonArray itemsToAdd = convertItemsToAdd(event.getAdd(), htmlNode,
                polymerModelPath, event.getIndex());
        PolymerUtils.splice(htmlNode, polymerModelPath, event.getIndex(),
                event.getRemove().length(), itemsToAdd);
    }

    private JsonArray convertItemsToAdd(JsArray<?> itemsToAdd, Element htmlNode,
            String polymerModelPath, int splitIndex) {
        JsonArray convertedItems = Json.createArray();
        for (int i = 0; i < itemsToAdd.length(); i++) {
            Object item = itemsToAdd.get(i);
            listenToSubPropertiesChanges(htmlNode, polymerModelPath,
                    splitIndex + i, item);
            convertedItems.set(i, PolymerUtils.convertToJson(item));
        }
        return convertedItems;
    }

    private void listenToSubPropertiesChanges(Element htmlNode,
            String polymerModelPath, int subNodeIndex, Object item) {
        if (item instanceof StateNode) {
            StateNode stateNode = (StateNode) item;
            NodeMap feature = null;
            if (stateNode.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
                feature = stateNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
            } else if (stateNode.hasFeature(NodeFeatures.BASIC_TYPE_VALUE)) {
                feature = stateNode.getMap(NodeFeatures.BASIC_TYPE_VALUE);
            }

            if (feature != null) {
                feature.addPropertyAddListener(event -> {
                    Command command = () -> PolymerUtils.setListValueByIndex(
                            htmlNode, polymerModelPath, subNodeIndex,
                            PolymerUtils.convertToJson(event.getProperty()));
                    invokeWhenNodeIsConstructed(command, stateNode);
                });
            }
        }
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
                .runWhenDependenciesChange(() -> user.use(property));

        bindings.set(name, computation);
    }

    private void updateProperty(MapProperty mapProperty, Element element) {
        if (PolymerUtils.isPolymerElement(element)) {
            // another way of property binding is used for polymer elements.
            return;
        }
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
        WidgetUtil.updateAttribute(element, name, mapProperty.getValue());
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
            EventRemover remover = context.htmlNode.addEventListener(eventType,
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
        Object currentValue = WidgetUtil.getJsProperty(context.htmlNode,
                propertyName);

        context.node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty(propertyName).syncToServer(currentValue);
    }

    private EventRemover bindChildren(BindingContext context) {
        NodeList children = context.node.getList(NodeFeatures.ELEMENT_CHILDREN);

        for (int i = 0; i < children.length(); i++) {
            StateNode childNode = (StateNode) children.get(i);

            Node child = context.binderContext.createAndBind(childNode);
            DomApi.wrap(context.htmlNode).appendChild(child);
        }

        return children.addSpliceListener(e -> {
            /*
             * Handle lazily so we can create the children we need to insert.
             * The change that gives a child node an element tag name might not
             * yet have been applied at this point.
             */
            Reactive.addFlushListener(() -> handleChildrenSplice(e, context));
        });
    }

    private EventRemover bindVirtualChildren(BindingContext context) {
        NodeList children = context.node.getList(NodeFeatures.VIRTUAL_CHILDREN);

        for (int i = 0; i < children.length(); i++) {
            appendVirtualChild(context, (StateNode) children.get(i), true);
        }

        return children.addSpliceListener(e -> {
            /*
             * Handle lazily so we can create the children we need to insert.
             * The change that gives a child node an element tag name might not
             * yet have been applied at this point.
             */
            Reactive.addFlushListener(() -> {
                JsArray<?> add = e.getAdd();
                if (!add.isEmpty()) {
                    for (int i = 0; i < add.length(); i++) {
                        appendVirtualChild(context, (StateNode) add.get(i),
                                true);
                    }
                }
            });
        });
    }

    private void appendVirtualChild(BindingContext context, StateNode node,
            boolean reactivePhase) {
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_DATA);
        JsonObject object = (JsonObject) map.getProperty(NodeProperties.PAYLOAD)
                .getValue();
        String type = object.getString(NodeProperties.TYPE);

        if (NodeProperties.IN_MEMORY_CHILD.equals(type)) {
            context.binderContext.createAndBind(node);
            return;
        }

        if (NodeProperties.INJECT_BY_ID.equals(type)) {
            if (PolymerUtils.getDomRoot(context.htmlNode) == null) {
                PolymerUtils.invokeWhenDefined(context.htmlNode,
                        () -> appendVirtualChild(context, node, false));
                node.getTree().getRegistry().getExistingElementMap().add(node);
                return;
            }

            String id = object.getString(NodeProperties.PAYLOAD);

            Element existingElement = PolymerUtils
                    .getDomElementById(context.htmlNode, id);
            verifyAttachedElement(existingElement, node, id, "id='" + id + "'",
                    context);

            node.setDomNode(existingElement);
            context.binderContext.createAndBind(node);
            node.getTree().getRegistry().getExistingElementMap().remove(node);
        } else if (NodeProperties.TEMPLATE_IN_TEMPLATE.equals(type)) {
            if (PolymerUtils.getDomRoot(context.htmlNode) == null) {
                PolymerUtils.invokeWhenDefined(context.htmlNode,
                        () -> appendVirtualChild(context, node, false));
                node.getTree().getRegistry().getExistingElementMap().add(node);
                return;
            }

            JsonArray path = object.getArray(NodeProperties.PAYLOAD);
            // TODO : check whether the element is already "attached"
            Element customElement = PolymerUtils.getCustomElement(
                    PolymerUtils.getDomRoot(context.htmlNode), path);
            verifyAttachedElement(customElement, node, null,
                    "path='" + path.toString() + "'", context);
            node.setDomNode(customElement);
            context.binderContext.createAndBind(node);
            node.getTree().getRegistry().getExistingElementMap().remove(node);
        } else {
            assert false : "Unexpected payload type " + type;
        }
        if (!reactivePhase) {
            // Correct binding requires reactive involvement which doesn't
            // happen automatically when we are out of the phase. So we should
            // call <code>flush()</code> explicitly.
            Reactive.flush();
        }
    }

    private void verifyAttachedElement(Element element, StateNode attachNode,
            String id, String address, BindingContext context) {
        StateNode node = context.node;

        String tag = getTag(attachNode);

        boolean failure = false;
        if (element == null) {
            failure = true;
            Console.warn("Element addressed by the " + address
                    + " is not found. The requested tag name is '" + tag + "'");
        } else if (!PolymerUtils.hasTag(element, tag)) {
            failure = true;
            Console.warn("Element addressed by the " + address
                    + " has the wrong tag name '" + element.getTagName()
                    + "', the requested tag name is '" + tag + "'");
        }

        if (failure) {
            node.getTree().sendExistingElementWithIdAttachToServer(node,
                    attachNode.getId(), -1, id);
        } else {
            NodeMap map = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);
            StateNode shadowRootNode = (StateNode) map
                    .getProperty(NodeProperties.SHADOW_ROOT).getValue();
            NodeList list = shadowRootNode
                    .getList(NodeFeatures.ELEMENT_CHILDREN);
            Integer existingId = null;

            for (int i = 0; i < list.length(); i++) {
                StateNode stateNode = (StateNode) list.get(i);
                Node domNode = stateNode.getDomNode();

                if (domNode.equals(element)) {
                    existingId = stateNode.getId();
                    break;
                }
            }

            if (existingId != null) {
                Console.warn("Element addressed by the " + address
                        + " has been already attached previously via the node id='"
                        + existingId + "'");
                node.getTree().sendExistingElementWithIdAttachToServer(node,
                        attachNode.getId(), existingId, id);
            }
        }
    }

    private static Optional<String> extractNodeId(StateNode node) {
        if (node.hasFeature(NodeFeatures.ELEMENT_ATTRIBUTES)) {
            return Optional.ofNullable(node
                    .getMap(NodeFeatures.ELEMENT_ATTRIBUTES)
                    .getProperty(NodeProperties.ID).getValueOrDefault(null));
        }
        return Optional.empty();
    }

    private void invokeWhenNodeIsConstructed(Command command, StateNode node) {
        Computation computation = Reactive.runWhenDependenciesChange(command);
        node.addUnregisterListener(event -> computation.stop());
    }

    private static void bindElementFromShadowRootByTagName(
            BinderContext binderContext, StateNode childNode, String childTag,
            ShadowRoot shadowRoot) {
        Node shadowRootElement = PolymerUtils
                .searchForElementInShadowRoot(shadowRoot, childTag);
        if (shadowRootElement == null) {
            throw new IllegalStateException(
                    "Could not locate element imported with @Id annotation, tag = '"
                            + childTag
                            + "', in shadow root of a parent element");
        }
        binderContext.bind(childNode, shadowRootElement);
    }

    private void handleChildrenSplice(ListSpliceEvent event,
            BindingContext context) {
        JsArray<?> remove = event.getRemove();
        for (int i = 0; i < remove.length(); i++) {
            StateNode childNode = (StateNode) remove.get(i);
            Node child = childNode.getDomNode();

            assert child != null : "Can't find element to remove";

            if (DomApi.wrap(child).getParentNode() == context.htmlNode) {
                DomApi.wrap(context.htmlNode).removeChild(child);
            }
            /*
             * If the client-side element is not inside the parent the server
             * thought it should be (because of client-side-only DOM changes),
             * nothing is done at this point. If the server appends the element
             * to a new parent, that will override the client DOM in the code
             * below.
             */
        }

        JsArray<?> add = event.getAdd();
        if (!add.isEmpty()) {
            addChildren(event.getIndex(), context, add);
        }
    }

    private void addChildren(int index, BindingContext context,
            JsArray<?> add) {
        NodeList nodeChildren = context.node
                .getList(NodeFeatures.ELEMENT_CHILDREN);

        Node beforeRef;
        if (index == 0) {
            // Insert at the first position
            beforeRef = DomApi.wrap(context.htmlNode).getFirstChild();
        } else if (index <= nodeChildren.length() && index > 0) {
            StateNode previousSibling = getPreviousSibling(index, context);
            // Insert before the next sibling of the current node
            beforeRef = previousSibling == null ? null
                    : DomApi.wrap(previousSibling.getDomNode())
                            .getNextSibling();
        } else {
            // Insert at the end
            beforeRef = null;
        }

        for (int i = 0; i < add.length(); i++) {
            Object newChildObject = add.get(i);
            StateNode newChild = (StateNode) newChildObject;

            Node childNode = context.binderContext.createAndBind(newChild);

            DomApi.wrap(context.htmlNode).insertBefore(childNode, beforeRef);

            beforeRef = DomApi.wrap(childNode).getNextSibling();
        }
    }

    private StateNode getPreviousSibling(int index, BindingContext context) {
        NodeList nodeChildren = context.node
                .getList(NodeFeatures.ELEMENT_CHILDREN);

        int count = 0;
        StateNode node = null;
        for (int i = 0; i < nodeChildren.length(); i++) {
            if (count == index) {
                return node;
            }
            node = (StateNode) nodeChildren.get(i);
            if (node.getDomNode() != null) {
                count++;
            }
        }
        return node;
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

        BOUND.delete(context.node);
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

        Computation computation = Reactive.runWhenDependenciesChange(() -> {
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

        EventRemover remover = context.htmlNode.addEventListener(eventType,
                event -> handleDomEvent(event, context.htmlNode, context.node),
                false);

        context.listenerRemovers.set(eventType, remover);
    }

    private NodeMap getDomEventListenerMap(StateNode node) {
        return node.getMap(NodeFeatures.ELEMENT_LISTENERS);
    }

    private void handleDomEvent(Event event, Node element, StateNode node) {
        assert element instanceof Element : "Cannot handle DOM event for a Node";
        String type = event.getType();

        NodeMap listenerMap = getDomEventListenerMap(node);

        ConstantPool constantPool = node.getTree().getRegistry()
                .getConstantPool();
        String expressionConstantKey = (String) listenerMap.getProperty(type)
                .getValue();
        assert expressionConstantKey != null;

        assert constantPool.has(expressionConstantKey);

        JsArray<String> dataExpressions = constantPool
                .get(expressionConstantKey);

        JsonObject eventData;
        if (dataExpressions.isEmpty()) {
            eventData = null;
        } else {
            eventData = Json.createObject();

            for (int i = 0; i < dataExpressions.length(); i++) {
                String expressionString = dataExpressions.get(i);

                EventDataExpression expression = getOrCreateExpression(
                        expressionString);

                JsonValue expressionValue = expression.evaluate(event,
                        (Element) element);

                eventData.put(expressionString, expressionValue);
            }
        }

        node.getTree().sendEventToServer(node, type, eventData);
    }

    private EventRemover bindClassList(Element element, StateNode node) {
        NodeList classNodeList = node.getList(NodeFeatures.CLASS_LIST);

        for (int i = 0; i < classNodeList.length(); i++) {
            DomApi.wrap(element).getClassList()
                    .add((String) classNodeList.get(i));
        }

        return classNodeList.addSpliceListener(e -> {
            DomTokenList classList = DomApi.wrap(element).getClassList();

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

    private EventRemover bindPolymerEventHandlerNames(BindingContext context) {
        return ServerEventHandlerBinder.bindServerEventHandlerNames(
                () -> WidgetUtil.crazyJsoCast(context.htmlNode), context.node,
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS);
    }

    private EventRemover bindClientDelegateMethods(BindingContext context) {
        assert context.htmlNode instanceof Element : "Cannot bind client delegate methods to a Node";
        return ServerEventHandlerBinder.bindServerEventHandlerNames(
                (Element) context.htmlNode, context.node);
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
