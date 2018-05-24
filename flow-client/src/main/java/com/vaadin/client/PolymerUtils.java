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

package com.vaadin.client;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.collection.JsWeakMap;
import com.vaadin.client.flow.dom.DomApi;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.ShadowRoot;
import elemental.html.HTMLCollection;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Utils class, intended to ease working with Polymer related code on a client
 * side.
 *
 * @author Vaadin Ltd.
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
     *      docs</a> for more info.
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
     *      docs</a> for more info.
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
    public static JsonValue convertToJson(Object object) {
        if (object instanceof StateNode) {
            StateNode node = (StateNode) object;
            NodeFeature feature = null;
            if (node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
                feature = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
            } else if (node.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
                feature = node.getList(NodeFeatures.TEMPLATE_MODELLIST);
            } else if (node.hasFeature(NodeFeatures.BASIC_TYPE_VALUE)) {
                return convertToJson(node.getMap(NodeFeatures.BASIC_TYPE_VALUE)
                        .getProperty(NodeProperties.VALUE));
            }
            assert feature != null : "Don't know how to convert node without map or list features";

            JsonValue convert = feature.convert(PolymerUtils::convertToJson);
            if (convert instanceof JsonObject
                    && !((JsonObject) convert).hasKey("nodeId")) {
                ((JsonObject) convert).put("nodeId", node.getId());
            }
            return convert;
        } else if (object instanceof MapProperty) {
            MapProperty property = (MapProperty) object;
            if (property.getMap().getId() == NodeFeatures.BASIC_TYPE_VALUE) {
                return convertToJson(property.getValue());
            } else {
                JsonObject convertedObject = Json.createObject();
                convertedObject.put(property.getName(),
                        convertToJson(property.getValue()));
                return convertedObject;
            }
        } else {
            return WidgetUtil.crazyJsoCast(object);
        }
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
        return (typeof $wnd.Polymer === 'function') && $wnd.Polymer.Element && htmlNode instanceof $wnd.Polymer.Element;
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

}
