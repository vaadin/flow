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
package com.vaadin.flow.template.angular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementStateProvider;
import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.flow.internal.StateNode;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A template AST node representing a regular element.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class ElementTemplateNode extends AbstractElementTemplateNode {
    /**
     * Type value for element template nodes in JSON messages.
     */
    public static final String TYPE = "element";

    private final String tag;

    private final ArrayList<TemplateNode> children;

    private final Map<String, BindingValueProvider> properties;
    private final Map<String, BindingValueProvider> attributes;
    private final Map<String, BindingValueProvider> classNames;
    private final Map<String, String> eventHandlers;

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
     * @param classNames
     *            a map of class name bindings for this node, not
     *            <code>null</code>
     * @param eventHandlers
     *            a map of event handlers for this node, not <code>null</code>
     * @param childBuilders
     *            a list of template builders for child nodes
     */
    public ElementTemplateNode(TemplateNode parent, String tag,
            Map<String, BindingValueProvider> properties,
            Map<String, BindingValueProvider> attributes,
            Map<String, BindingValueProvider> classNames,
            Map<String, String> eventHandlers,
            List<TemplateNodeBuilder> childBuilders) {
        super(parent);
        assert tag != null;
        assert properties != null;
        assert attributes != null;
        assert classNames != null;
        assert childBuilders != null;
        assert eventHandlers != null;

        this.tag = tag;

        // Defensive copies
        this.properties = new HashMap<>(properties);
        this.attributes = new HashMap<>(attributes);
        this.classNames = new HashMap<>(classNames);
        this.eventHandlers = new HashMap<>(eventHandlers);

        children = new ArrayList<>(childBuilders.size());
        childBuilders.stream()
                .flatMap(childBuilder -> childBuilder.build(this).stream())
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
     * Gets all the class names that have a binding for this element.
     *
     * @return the class names
     */
    public Stream<String> getClassNames() {
        return classNames.keySet().stream();
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
     * Gets the class binding for the given name.
     *
     * @param name
     *            the class name
     * @return an optional template binding for the class name, empty if there
     *         is no class binding with the given name
     */
    public Optional<BindingValueProvider> getClassNameBinding(String name) {
        return Optional.ofNullable(classNames.get(name));
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

        bindingsToJson(properties).ifPresent(
                propertiesJson -> json.put("properties", propertiesJson));

        bindingsToJson(attributes).ifPresent(
                attributesJson -> json.put("attributes", attributesJson));

        bindingsToJson(classNames).ifPresent(
                classNamesJson -> json.put("classNames", classNamesJson));

        mapToJson(eventHandlers, Json::create)
                .ifPresent(eventHandlersJson -> json.put("eventHandlers",
                        eventHandlersJson));

        // Super class takes care of the children
    }

    private static Optional<JsonObject> bindingsToJson(
            Map<String, BindingValueProvider> bindings) {
        return mapToJson(bindings, BindingValueProvider::toJson);
    }

    private static <T> Optional<JsonObject> mapToJson(Map<String, T> map,
            Function<T, JsonValue> toJson) {
        if (map.isEmpty()) {
            return Optional.empty();
        } else {
            JsonObject json = Json.createObject();

            map.forEach((name, value) -> json.put(name, toJson.apply(value)));

            return Optional.of(json);
        }
    }

    @Override
    public Optional<Element> findElement(StateNode stateNode, String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        if (attributes.containsKey("id")) {
            BindingValueProvider binding = attributes.get("id");
            if (id.equals(binding.getValue(stateNode))) {
                return Optional.of(getElement(0, stateNode));
            }
        }

        List<Element> elements = children.stream()
                .map(child -> child.findElement(stateNode, id))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

        if (elements.isEmpty()) {
            return Optional.empty();
        } else if (elements.size() == 1) {
            return Optional.of(elements.get(0));
        } else {
            throw new IllegalArgumentException("Multiple (" + elements.size()
                    + ") elements were found when looking for an element with id '"
                    + id + "'");
        }
    }
}
