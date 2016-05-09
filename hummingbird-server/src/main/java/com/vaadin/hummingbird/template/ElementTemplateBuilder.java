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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Builder for element template nodes.
 *
 * @author Vaadin Ltd
 */
public class ElementTemplateBuilder implements TemplateNodeBuilder {

    private final String tag;
    private final Map<String, BindingValueProvider> properties = new HashMap<>();
    private final Map<String, BindingValueProvider> attributes = new HashMap<>();
    private final List<TemplateNodeBuilder> children = new ArrayList<>();
    private final Map<String, String> eventHandlers = new HashMap<>();

    /**
     * Creates an element node builder with the given tag name.
     *
     * @param tag
     *            the tag name of the element, not <code>null</code>
     */
    public ElementTemplateBuilder(String tag) {
        assert tag != null;
        this.tag = tag;
    }

    @Override
    public ElementTemplateNode build(TemplateNode parent) {
        return new ElementTemplateNode(parent, getTag(), getProperties(),
                getAttributes(), getEventHandlers(), children);
    }

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name, not <code>null</code>
     */
    public String getTag() {
        return tag;
    }

    /**
     * Adds a property binding to this builder.
     *
     * @param name
     *            the name of the property, not <code>null</code>
     * @param binding
     *            the binding that will provide the property value, not
     *            <code>null</code>
     * @return this element template builder
     */
    public ElementTemplateBuilder setProperty(String name,
            BindingValueProvider binding) {
        assert name != null;
        assert binding != null;
        assert !properties.containsKey(
                name) : "There is already a property named " + name;

        properties.put(name, binding);
        return this;
    }

    /**
     * Adds an attribute binding to this builder.
     *
     * @param name
     *            the name of the attribute, not <code>null</code>
     * @param binding
     *            the binding that will provide the attribute value, not
     *            <code>null</code>
     * @return this element template builder
     */
    public ElementTemplateBuilder setAttribute(String name,
            BindingValueProvider binding) {
        assert name != null;
        assert binding != null;
        assert !attributes.containsKey(
                name) : "There is already an attribute named " + name;

        attributes.put(name, binding);
        return this;
    }

    /**
     * Gets the property bindings that have been defined using
     * {@link #setProperty(String, BindingValueProvider)}.
     *
     * @return a map of property bindings, not <code>null</code>
     */
    public Map<String, BindingValueProvider> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Gets the attribute bindings that have been defined using
     * {@link #setAttribute(String, BindingValueProvider)}.
     *
     * @return a map of attribute bindings, not <code>null</code>
     */
    public Map<String, BindingValueProvider> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Adds a builder for a child template node.
     *
     * @param childBuilder
     *            the child template node builder to add
     * @return this element template builder
     */
    public ElementTemplateBuilder addChild(TemplateNodeBuilder childBuilder) {
        assert childBuilder != null;

        children.add(childBuilder);
        return this;
    }

    /**
     * Gets the child builders that have been defined using
     * {@link #addChild(TemplateNodeBuilder)}.
     *
     * @return a stream of child template builders
     */
    public Stream<TemplateNodeBuilder> getChildren() {
        return children.stream();
    }

    /**
     * Adds an event handler to this builder.
     *
     * @param event
     *            the event name, not <code>null</code>
     * @param handler
     *            the handler (JS code) that will be called when the event is
     *            triggered, not <code>null</code>
     * @return this element template builder
     */
    public void setEventHandler(String event, String handler) {
        eventHandlers.put(event, handler);
    }

    /**
     * Gets the event handler that have been defined using
     * {@link #setEventHandler(String, String)}.
     *
     * @return a map of event handlers, not <code>null</code>
     */
    public Map<String, String> getEventHandlers() {
        return Collections.unmodifiableMap(eventHandlers);
    }
}
