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

package com.vaadin.client;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.collection.JsWeakMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.nodefeature.ListSpliceEvent;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.ShadowRoot;
import elemental.events.EventRemover;
import elemental.html.HTMLCollection;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Utils class, intended to ease working with Polymer related code on a client
 * side.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public final class PolymerUtils {

    private static JsWeakMap<Element, JsSet<Runnable>> readyListeners;

    private PolymerUtils() {
    }

    /**
     * Sets new value for list element for specified {@code htmlNode}.
     *
     * @param htmlNode
     *            node to call set method on
     * @param path
     *            polymer model path to property
     * @param listIndex
     *            list index to set element into
     * @param newValue
     *            new value to be set at desired index
     *
     * @see <a href=
     *      "https://www.polymer-project.org/2.0/docs/devguide/model-data">Polymer
     *      docs</a>
     */
    public static native void setListValueByIndex(Element htmlNode, String path,
            int listIndex, JsonValue newValue)
    /*-{
        htmlNode.set(path + "." + listIndex, newValue);
    }-*/;

    /**
     * Calls Polymer {@code splice} method on specified {@code htmlNode}.
     *
     * Splice call is made via {@code apply} method in order to force the method
     * to treat {@code itemsToAdd} as numerous parameters, not a single one.
     *
     * @param htmlNode
     *            node to call splice method on
     * @param path
     *            polymer model path to property
     * @param startIndex
     *            start index of a list for splice operation
     * @param deleteCount
     *            number of elements to delete from the list after startIndex
     * @param itemsToAdd
     *            elements to add after startIndex
     *
     * @see <a href=
     *      "https://www.polymer-project.org/2.0/docs/devguide/model-data">Polymer
     *      docs</a>
     */
    public static native void splice(Element htmlNode, String path,
            int startIndex, int deleteCount, JsonArray itemsToAdd)
    /*-{
        htmlNode.splice.apply(htmlNode, [path, startIndex, deleteCount].concat(itemsToAdd));
    }-*/;

    /**
     * Store the StateNode.id into the polymer property under 'nodeId'
     *
     * @param domNode
     *            polymer dom node
     * @param id
     *            id of a state node
     * @param path
     *            polymer model path to property
     */
    public static native void storeNodeId(Node domNode, int id, String path)
    /*-{
        if (typeof(domNode.get) !== 'undefined') {
            var polymerProperty = domNode.get(path);
            if (typeof(polymerProperty) === 'object'
                && polymerProperty["nodeId"] === undefined) {
                polymerProperty["nodeId"] = id;
            }
        }
    }-*/;

    /**
     * Makes an attempt to convert an object into json.
     *
     * @param object
     *            the object to convert to json
     * @return json from object, {@code null} for null
     */
    public static JsonValue createModelTree(Object object) {
        if (object instanceof StateNode) {
            StateNode node = (StateNode) object;
            NodeFeature feature = null;
            if (node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
                feature = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
            } else if (node.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
                feature = node.getList(NodeFeatures.TEMPLATE_MODELLIST);
            } else if (node.hasFeature(NodeFeatures.BASIC_TYPE_VALUE)) {
                return createModelTree(
                        node.getMap(NodeFeatures.BASIC_TYPE_VALUE)
                                .getProperty(NodeProperties.VALUE));
            }
            assert feature != null : "Don't know how to convert node without map or list features";

            JsonValue convert = feature.convert(PolymerUtils::createModelTree);
            if (convert instanceof JsonObject
                    && !((JsonObject) convert).hasKey("nodeId")) {

                ((JsonObject) convert).put("nodeId", node.getId());
                registerChangeHandlers(node, feature, convert);
            }
            return convert;
        } else if (object instanceof MapProperty) {
            MapProperty property = (MapProperty) object;
            if (property.getMap().getId() == NodeFeatures.BASIC_TYPE_VALUE) {
                return createModelTree(property.getValue());
            } else {
                JsonObject convertedObject = Json.createObject();
                convertedObject.put(property.getName(),
                        createModelTree(property.getValue()));
                return convertedObject;
            }
        } else {
            return WidgetUtil.crazyJsoCast(object);
        }
    }

    private static void registerChangeHandlers(StateNode node,
            NodeFeature feature, JsonValue value) {

        JsArray<EventRemover> registrations = JsCollections.array();
        if (node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
            assert feature instanceof NodeMap : "Received an inconsistent NodeFeature for a node that has a ELEMENT_PROPERTIES feature. It should be NodeMap, but it is: "
                    + feature;
            NodeMap map = (NodeMap) feature;
            registerPropertyChangeHandlers(value, registrations,
                    map);
            registerPropertyAddHandler(value, registrations, map);
        } else if (node.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
            assert feature instanceof NodeList : "Received an inconsistent NodeFeature for a node that has a TEMPLATE_MODELLIST feature. It should be NodeList, but it is: "
                    + feature;
            NodeList list = (NodeList) feature;
            registrations.push(list.addSpliceListener(
                    event -> handleListChange(event, value)));
        }
        assert !registrations
                .isEmpty() : "Node should have ELEMENT_PROPERTIES or TEMPLATE_MODELLIST feature";

        registrations.push(node.addUnregisterListener(
                event -> registrations.forEach(EventRemover::remove)));
    }

    private static void registerPropertyAddHandler(JsonValue value,
            JsArray<EventRemover> registrations, NodeMap map) {
        registrations.push(map.addPropertyAddListener(event -> {
            MapProperty property = event.getProperty();
            registrations.push(property.addChangeListener(
                    change -> handlePropertyChange(property, value)));
            handlePropertyChange(property, value);
        }));
    }

    private static void registerPropertyChangeHandlers(
            JsonValue value, JsArray<EventRemover> registrations, NodeMap map) {
        map.forEachProperty((property, propertyName) -> registrations
                .push(property.addChangeListener(
                        event -> handlePropertyChange(property, value))));
    }

    private static void handleListChange(ListSpliceEvent event,
            JsonValue value) {
        Reactive.addFlushListener(() -> doHandleListChange(event, value));
    }

    private static void doHandleListChange(ListSpliceEvent event,
            JsonValue value) {
        JsArray<?> add = event.getAdd();
        int index = event.getIndex();
        int remove = event.getRemove().length();
        StateNode node = event.getSource().getNode();
        StateNode root = getFirstParentWithDomNode(node);
        if (root == null) {
            Console.warn("Root node for node " + node.getId()
                    + " could not be found");
            return;
        }

        JsArray<Object> array = JsCollections.array();
        add.forEach(item -> array.push(createModelTree(item)));

        if (isPolymerElement((Element) root.getDomNode())) {
            String path = getNotificationPath(root, node, null);
            if (path != null) {

                splice((Element) root.getDomNode(), path, index, remove,
                        WidgetUtil.crazyJsoCast(array));
                return;
            }
        }
        @SuppressWarnings("unchecked")
        JsArray<Object> payload = (JsArray<Object>) value;
        payload.spliceArray(index, remove, array);
    }

    private static void handlePropertyChange(MapProperty property,
            JsonValue bean) {
        Reactive.addFlushListener(() -> doHandlePropertyChange(property, bean));
    }

    private static void doHandlePropertyChange(MapProperty property,
            JsonValue value) {
        String propertyName = property.getName();
        StateNode node = property.getMap().getNode();
        StateNode root = getFirstParentWithDomNode(node);
        if (root == null) {
            Console.warn("Root node for node " + node.getId()
                    + " could not be found");
            return;
        }
        JsonValue modelTree = createModelTree(property.getValue());

        if (isPolymerElement((Element) root.getDomNode())) {
            String path = getNotificationPath(root, node, propertyName);
            if (path != null) {
                setProperty((Element) root.getDomNode(), path, modelTree);
            }
            return;
        }
        WidgetUtil.setJsProperty(value, propertyName, modelTree);
    }

    private static String getNotificationPath(StateNode rootNode,
            StateNode currentNode, String propertyName) {

        JsArray<String> path = JsCollections.array();
        if (propertyName != null) {
            path.push(propertyName);
        }
        return doGetNotificationPath(rootNode, currentNode, path);
    }

    private static String doGetNotificationPath(StateNode rootNode,
            StateNode currentNode, JsArray<String> path) {

        StateNode parent = currentNode.getParent();
        if (parent.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
            String propertyPath = getPropertiesNotificationPath(currentNode);
            if (propertyPath == null) {
                return null;
            }
            path.push(propertyPath);
        } else if (parent.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
            String listPath = getListNotificationPath(currentNode);
            if (listPath == null) {
                return null;
            }
            path.push(listPath);
        }
        if (!parent.equals(rootNode)) {
            return doGetNotificationPath(rootNode, parent, path);
        }

        StringBuilder pathBuilder = new StringBuilder();
        String sep = "";
        for (int i = path.length() - 1; i >= 0; i--) {
            pathBuilder.append(sep).append(path.get(i));
            sep = ".";
        }
        return pathBuilder.toString();
    }

    private static String getListNotificationPath(StateNode currentNode) {
        int indexInTheList = -1;
        NodeList children = currentNode.getParent()
                .getList(NodeFeatures.TEMPLATE_MODELLIST);

        for (int i = 0; i < children.length(); i++) {
            Object object = children.get(i);
            if (currentNode.equals(object)) {
                indexInTheList = i;
                break;
            }
        }

        if (indexInTheList < 0) {
            return null;
        }
        return String.valueOf(indexInTheList);
    }

    private static String getPropertiesNotificationPath(StateNode currentNode) {
        String propertyNameInTheMap = null;
        NodeMap map = currentNode.getParent()
                .getMap(NodeFeatures.ELEMENT_PROPERTIES);

        JsArray<String> propertyNames = map.getPropertyNames();
        for (int i = 0; i < propertyNames.length(); i++) {
            String propertyName = propertyNames.get(i);
            if (currentNode.equals(map.getProperty(propertyName).getValue())) {
                propertyNameInTheMap = propertyName;
                break;
            }
        }
        if (propertyNameInTheMap == null) {
            return null;
        }
        return propertyNameInTheMap;
    }

    /**
     * Gets the first parent node that also has a DOM Node attached to it.
     * 
     * @param node
     *            the node
     * @return the first parent node with a DOM Node, or <code>null</code> if
     *         none can be found
     */
    private static StateNode getFirstParentWithDomNode(StateNode node) {
        StateNode parent = node.getParent();
        while (parent != null && parent.getDomNode() == null) {
            parent = parent.getParent();
        }
        return parent;
    }

    /**
     * Checks whether the {@code htmlNode} is a polymer 2 element.
     *
     * @param htmlNode
     *            HTML element to check
     * @return {@code true} if the {@code htmlNode} is a polymer element
     */
    public static native boolean isPolymerElement(Element htmlNode)
    /*-{
        var isP2Element = (typeof $wnd.Polymer === 'function') && $wnd.Polymer.Element && htmlNode instanceof $wnd.Polymer.Element;
        var isP3Element = htmlNode.constructor.polymerElementVersion !== undefined;

        return (isP2Element || isP3Element);
    }-*/;

    /**
     * Checks whether the {@code htmlNode} can turn into polymer 2 element
     * later.
     * <p>
     * Lazy loaded dependencies can load Polymer later than the element itself
     * gets processed by the Flow. This method helps to determine such elements.
     *
     * @param htmlNode
     *            HTML element to check
     * @return {@code true} if the {@code htmlNode} can become a polymer 2
     *         element
     */
    public static native boolean mayBePolymerElement(Element htmlNode)
    /*-{
        return $wnd.customElements && htmlNode.localName.indexOf('-') > -1;
    }-*/;

    /**
     * Get first element by css query in the shadow root provided.
     *
     * @param shadowRoot
     *            shadow root element
     * @param cssQuery
     *            css query
     * @return first element matching the query or {@code null} for no matches
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Web_Components/Shadow_DOM">https://developer.mozilla.org/en-US/docs/Web/Web_Components/Shadow_DOM</a>
     */
    public static native Node searchForElementInShadowRoot(
            ShadowRoot shadowRoot, String cssQuery)
    /*-{
        return shadowRoot.querySelector(cssQuery);
    }-*/;

    /**
     * Get the element by id from the shadow root provided.
     *
     * @param shadowRoot
     *            shadow root element
     * @param id
     *            element id
     * @return the element with id provided or {@code null} for no matches
     *
     * @see <a href=
     *      "http://html5index.org/Shadow%20DOM%20-%20ShadowRoot.html">http://html5index.org/Shadow%20DOM%20-%20ShadowRoot.html</a>
     */
    public static native Node getElementInShadowRootById(ShadowRoot shadowRoot,
            String id)
    /*-{
        return shadowRoot.getElementById(id);
    }-*/;

    /**
     * Find the DOM element inside shadow root of the {@code shadowRootParent}.
     *
     * @param shadowRootParent
     *            the parent whose shadow root contains the element with the
     *            {@code id}
     * @param id
     *            the identifier of the element to search for
     * @return the element with the given {@code id} inside the shadow root of
     *         the parent
     */
    public static native Element getDomElementById(Node shadowRootParent,
            String id)
    /*-{
        return shadowRootParent.$[id];
    }-*/;

    /**
     * Returns {@code true} if the DOM structure of the polymer custom element
     * {@code shadowRootParent} is ready (meaning that it has shadow root and
     * its shadow root may be queried for children referenced by id).
     *
     * @param shadowRootParent
     *            the polymer custom element
     * @return {@code true} if the {@code shadowRootParent} element is ready
     */
    public static native boolean isReady(Node shadowRootParent)
    /*-{
        return typeof(shadowRootParent.$) != "undefined";
    }-*/;

    /**
     * Checks whether the {@code node} has required {@code tag}.
     *
     * @param node
     *            the node to check
     * @param tag
     *            the required tag name
     * @return {@code true} if the node has required tag name
     */
    public static boolean hasTag(Node node, String tag) {
        return node instanceof Element
                && tag.equalsIgnoreCase(((Element) node).getTagName());
    }

    /**
     * Gets the custom element using {@code path} of indices starting from the
     * {@code root}.
     *
     * @param root
     *            the root element to start from
     * @param path
     *            the indices path identifying the custom element.
     * @return the element inside the {@code root} by the path of indices
     */
    public static Element getCustomElement(Node root, JsonArray path) {
        Node current = root;
        for (int i = 0; i < path.length(); i++) {
            JsonValue value = path.get(i);
            current = getChildIgnoringStyles(current, (int) value.asNumber());
        }
        if (current instanceof Element) {
            return (Element) current;
        } else if (current == null) {
            Console.warn(
                    "There is no element addressed by the path '" + path + "'");
        } else {
            Console.warn("The node addressed by path " + path
                    + " is not an Element");
        }
        return null;
    }

    /**
     * Returns the shadow root of the {@code templateElement}.
     *
     * @param templateElement
     *            the owner of the shadow root
     * @return the shadow root of the element
     */
    public static native Element getDomRoot(Node templateElement)
    /*-{
        return templateElement.root;
    }-*/;

    /**
     * Invokes the {@code runnable} when the custom element with the given
     * {@code tagName} is initialized (its DOM structure becomes available).
     *
     * @param tagName
     *            the name of the custom element
     * @param runnable
     *            the command to run when the element if initialized
     */
    public static native void invokeWhenDefined(String tagName,
            Runnable runnable)
    /*-{
        $wnd.customElements.whenDefined(tagName).then(
            function () {
                runnable.@java.lang.Runnable::run(*)();
            });
    }-*/;

    /**
     * Gets the tag name of the {@code node}.
     *
     * @param node
     *            the node to get the tag name from
     * @return the tag name of the node
     */
    public static String getTag(StateNode node) {
        return (String) node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).getValue();
    }

    /**
     * Adds the {@code listener} which will be invoked when the
     * {@code polymerElement} becomes "ready" meaning that it's method
     * {@code ready} is called.
     * <p>
     * The listener won't be called if the element is already "ready" and the
     * listener will be removed immediately once it's executed.
     *
     * @param polymerElement
     *            the custom (polymer) element to listen its readiness state
     * @param listener
     *            the callback to execute once the element becomes ready
     */
    public static void addReadyListener(Element polymerElement,
            Runnable listener) {
        if (readyListeners == null) {
            readyListeners = JsCollections.weakMap();
        }
        JsSet<Runnable> set = readyListeners.get(polymerElement);
        if (set == null) {
            set = JsCollections.set();
            readyListeners.set(polymerElement, set);
        }
        set.add(listener);
    }

    /**
     * Fires the ready event for the {@code polymerElement}.
     *
     * @param polymerElement
     *            the custom (polymer) element whose state is "ready"
     */
    public static void fireReadyEvent(Element polymerElement) {
        if (readyListeners == null) {
            return;
        }
        JsSet<Runnable> listeners = readyListeners.get(polymerElement);
        if (listeners != null) {
            readyListeners.delete(polymerElement);
            listeners.forEach(Runnable::run);
        }
    }

    private static Node getChildIgnoringStyles(Node parent, int index) {
        HTMLCollection children = DomApi.wrap(parent).getChildren();
        int filteredIndex = -1;
        for (int i = 0; i < children.getLength(); i++) {
            Node next = children.item(i);
            assert next instanceof Element : "Unexpected element type in the collection of children. "
                    + "DomElement::getChildren is supposed to return Element chidren only, but got "
                    + next.getClass();
            Element element = (Element) next;
            if (!"style".equalsIgnoreCase(element.getTagName())) {
                filteredIndex++;
            }
            if (filteredIndex == index) {
                return next;
            }
        }
        return null;
    }

    /**
     * Sets a property to an element by using the Polymer {@code set} method.
     * 
     * @param element
     *            the element to set the property to
     * @param path
     *            the path of the property
     * @param value
     *            the value
     */
    public static native void setProperty(Element element, String path,
            Object value)
    /*-{
         element.set(path, value);
     }-*/;

}
