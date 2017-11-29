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
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.flow.nodefeature.NodeFeatures;
import com.vaadin.flow.nodefeature.NodeProperties;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.ShadowRoot;
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
     * Lazy loaded dependencies can load Polymer later than
     * the element itself gets processed by the Flow. This method helps to
     * determine such elements.
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
}
