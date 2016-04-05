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
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;

/**
 * A template AST node representing a regular element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementTemplateNode extends TemplateNode {
    private final String tag;

    private final ArrayList<TemplateNode> children = new ArrayList<>();

    private final HashMap<String, TemplateBinding> attributes;

    /**
     * Creates a new template node from a builder.
     *
     * @param builder
     *            the element template builder to use
     */
    public ElementTemplateNode(ElementTemplateBuilder builder) {
        tag = builder.getTag();

        // Copy to remain immutable
        attributes = new HashMap<>(builder.getAttributes());

        builder.getChildren().map(TemplateBuilder::build).forEach(child -> {
            child.init(this);
            children.add(child);
        });
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
     * Gets all the attribute names defined for this element.
     *
     * @return the attribute names
     */
    public Stream<String> getAttributeNames() {
        return attributes.keySet().stream();
    }

    /**
     * Gets the attribute binding for the given name.
     *
     * @param name
     *            the attribute name
     * @return an optional template binding for the attribute, empty if there is
     *         no attribute with the given name
     */
    public Optional<TemplateBinding> getAttributeBinding(String name) {
        return Optional.ofNullable(attributes.get(name));
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
}
