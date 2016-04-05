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

/**
 * A template AST node representing a regular element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementTemplateNode extends TemplateNode {
    private final String tag;

    private final ArrayList<TemplateNode> children;

    private final HashMap<String, TemplateBinding> properties;

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
     * @param childBuilders
     *            a list of template builders for child nodes
     */
    public ElementTemplateNode(TemplateNode parent, String tag,
            Map<String, TemplateBinding> properties,
            List<TemplateNodeBuilder> childBuilders) {
        super(parent);
        assert tag != null;
        assert properties != null;
        assert childBuilders != null;

        this.tag = tag;

        // Defensive copy
        this.properties = new HashMap<>(properties);

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
     * Gets the property binding for the given name.
     *
     * @param name
     *            the property name
     * @return an optional template binding for the proeprty, empty if there is
     *         no attribute with the given name
     */
    public Optional<TemplateBinding> getPropertyBinding(String name) {
        return Optional.ofNullable(properties.get(name));
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TemplateNode getChild(int childIndex) {
        return children.get(childIndex);
    }
}
