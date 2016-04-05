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

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A node in the AST parsed from a template file. A template node is immutable.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class TemplateNode implements Serializable {
    private static final AtomicInteger nextId = new AtomicInteger();

    private final int id = nextId.incrementAndGet();

    private final TemplateNode parent;

    /**
     * Creates a new template node with the given node as its parent.
     *
     * @param parent
     *            the parent of the new template node, or null if the node is
     *            the root of a template tree
     */
    public TemplateNode(TemplateNode parent) {
        this.parent = parent;
    }

    /**
     * Gets the parent node of this node.
     *
     * @return an optional parent node, or an empty optional if this node is the
     *         root of a template tree
     */
    public Optional<TemplateNode> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Gets the globally unique id of this template node.
     *
     * @return the template node id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the number of child nodes.
     *
     * @return the number of child nodes
     */
    public abstract int getChildCount();

    /**
     * Gets the child node at the given index.
     *
     * @param index
     *            the child index
     * @return the child at the given index
     */
    public abstract TemplateNode getChild(int index);
}
