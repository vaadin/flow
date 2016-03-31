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
 * @since
 * @author Vaadin Ltd
 */
public class ElementTemplateBuilder implements TemplateBuilder {

    private final String tag;
    private final Map<String, TemplateBinding> attributes = new HashMap<>();
    private final List<TemplateBuilder> children = new ArrayList<>();

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
    public ElementTemplateNode build() {
        return new ElementTemplateNode(this);
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
     * Adds an attribute binding to this builder.
     *
     * @param name
     *            the name of the attribute, not <code>null</code>
     * @param binding
     *            the binding that will provide the attribute value, not
     *            <code>null</code>
     * @return this element template builder
     */
    public ElementTemplateBuilder addAttribute(String name,
            TemplateBinding binding) {
        assert name != null;
        assert binding != null;
        assert !attributes.containsKey(
                name) : "There is already an attribute named " + name;

        attributes.put(name, binding);
        return this;
    }

    /**
     * Gets the attribute bindings that have been defined using
     * {@link #addAttribute(String, TemplateBinding)}.
     *
     * @return a map of attribute bindings, not <code>null</code>
     */
    public Map<String, TemplateBinding> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Adds a builder for a child template node.
     *
     * @param childBuilder
     *            the child template node builder to add
     * @return this element template builder
     */
    public ElementTemplateBuilder addChild(TemplateBuilder childBuilder) {
        assert childBuilder != null;

        children.add(childBuilder);
        return this;
    }

    /**
     * Gets the child builders that have been defined using
     * {@link #addChild(TemplateBuilder)}.
     *
     * @return a stream of child template builders
     */
    public Stream<TemplateBuilder> getChildren() {
        return children.stream();
    }
}
