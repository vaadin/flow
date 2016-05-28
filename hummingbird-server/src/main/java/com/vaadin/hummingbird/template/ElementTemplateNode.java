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
package com.vaadin.hummingbird.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * A template AST node representing a regular element.
 *
 * @author Vaadin Ltd
 */
public class ElementTemplateNode extends AbstractElementTemplateNode {
    /**
     * Type value for element template nodes in JSON messages.
     */
    public static final String TYPE = "element";

    private final String tag;

    private final ArrayList<TemplateNode> children;

    private final HashMap<String, BindingValueProvider> properties;
    private final HashMap<String, BindingValueProvider> attributes;
    private final HashMap<String, String> eventHandlers;

    /**
     * Creates a new template node.
     *
     * @param parent
     *            the parent node of this node, or <code>null</code> if this
     *            node is the root of a template tree
     * @param tag
     *            the tag of the element, not <code>null</code>
     * @param properties
     *            a map of property bindings for this node, not
     *            <code>null</code>
     * @param attributes
     *            a map of attribute bindings for this node, not
     *            <code>null</code>
     * @param eventHandlers
     *            a map of event handlers for this node, not <code>null</code>
     * @param childBuilders
     *            a list of template builders for child nodes
     */
    public ElementTemplateNode(TemplateNode parent, String tag,
            Map<String, BindingValueProvider> properties,
            Map<String, BindingValueProvider> attributes,
            Map<String, String> eventHandlers,
            List<TemplateNodeBuilder> childBuilders) {
        super(parent);
        assert tag != null;
        assert properties != null;
        assert childBuilders != null;
        assert eventHandlers != null;

        this.tag = tag;

        // Defensive copies
        this.properties = new HashMap<>(properties);
        this.attributes = new HashMap<>(attributes);
        this.eventHandlers = new HashMap<>(eventHandlers);

        children = new ArrayList<>(childBuilders.size());
        childBuilders.stream().map(childBuilder -> childBuilder.build(this))
                .forEach(children::add);
    }

    /**
     * Gets the tag name of this element.
     *
     * @return the tag name, not <code>null</code>
     */
    public String getTag() {
        return tag;
    }

    /**
     * Gets all the property names defined for this element.
     *
     * @return the property names
     */
    public Stream<String> getPropertyNames() {
        return properties.keySet().stream();
    }

    /**
     * Gets all the attribute names defined for this element.
     *
     * @return the attribute names
     */
    public Stream<String> getAttributeNames() {
        return attributes.keySet().stream();
    }

    /**
     * Gets all the event names defined for this element.
     *
     * @return the event names
     */
    public Stream<String> getEventNames() {
        return eventHandlers.keySet().stream();
    }

    /**
     * Gets the property binding for the given name.
     *
     * @param name
     *            the property name
     * @return an optional template binding for the property, empty if there is
     *         no property with the given name
     */
    public Optional<BindingValueProvider> getPropertyBinding(String name) {
        return Optional.ofNullable(properties.get(name));
    }

    /**
     * Gets the attribute binding for the given name.
     *
     * @param name
     *            the attribute name
     * @return an optional template binding for the attribute, empty if there is
     *         no attribute with the given name
     */
    public Optional<BindingValueProvider> getAttributeBinding(String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * Gets the event handler expression for the given event name, if present.
     *
     * @param event
     *            the event name
     * @return an optional event handler expression for the event, empty if
     *         there is no event with the given name
     */
    public Optional<String> getEventHandlerExpression(String event) {
        return Optional.ofNullable(eventHandlers.get(event));
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TemplateNode getChild(int childIndex) {
        return children.get(childIndex);
    }

    @Override
    protected ElementStateProvider createStateProvider() {
        return new TemplateElementStateProvider(this);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put(TemplateNode.KEY_TYPE, TYPE);

        json.put("tag", tag);

        if (!properties.isEmpty()) {
            JsonObject propertiesJson = Json.createObject();

            properties.forEach((name, binding) -> propertiesJson.put(name,
                    binding.toJson()));

            json.put("properties", propertiesJson);
        }

        if (!attributes.isEmpty()) {
            JsonObject attributesJson = Json.createObject();

            attributes.forEach((name, binding) -> attributesJson.put(name,
                    binding.toJson()));

            json.put("attributes", attributesJson);
        }

        if (!eventHandlers.isEmpty()) {
            JsonObject eventsJson = Json.createObject();

            eventHandlers.forEach(
                    (event, handler) -> eventsJson.put(event, handler));

            json.put("eventHandlers", eventsJson);

        }

        // Super class takes care of the children
    }

    @Override
    public Optional<Element> findElementById(StateNode stateNode, String id) {
        if (attributes.containsKey("id")) {
            BindingValueProvider binding = attributes.get("id");
            if (id.equals(binding.getValue(stateNode))) {
                return Optional.of(getElement(0, stateNode));
            }
        }

        for (int i = 0; i < getChildCount(); i++) {
            TemplateNode child = getChild(i);
            Optional<Element> e = child.findElementById(stateNode, id);
            if (e.isPresent()) {
                return e;
            }
        }

        return Optional.empty();
    }
}
