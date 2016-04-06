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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.hummingbird.dom.ElementStateProvider;

/**
 * A node in the AST parsed from a template file. A template node is immutable.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class TemplateNode implements Serializable {
    private static final AtomicInteger nextId = new AtomicInteger();

    private static final ConcurrentHashMap<Integer, TemplateNode> registry = new ConcurrentHashMap<>();

    private final int id = nextId.incrementAndGet();

    private final TemplateNode parent;

    private final ElementStateProvider stateProvider;

    /**
     * Creates a new template node with the given node as its parent.
     *
     * @param parent
     *            the parent of the new template node, or null if the node is
     *            the root of a template tree
     */
    public TemplateNode(TemplateNode parent) {
        this.parent = parent;

        registry.put(Integer.valueOf(id), this);

        stateProvider = createStateProvider(this);
    }

    /*
     * Fulgy hack just to avoid complaints from sonarcube about calling
     * overrideable methods in the constructor, which is exactly what we want to
     * do in this case even though it's generally a dangerous operation.
     */
    private static ElementStateProvider createStateProvider(TemplateNode node) {
        ElementStateProvider provider = node.createStateProvider();

        assert provider != null;

        return provider;
    }

    /**
     * Gets the parent node of this node, or <code>null</code> if this is the
     * root node.
     *
     * @return the parent node, or <code>null</code>
     */
    public TemplateNode getParent() {
        return parent;
    }

    /**
     * Gets the globally unique id of this template node. A node can be found
     * based on its id using {@link #get(int)}.
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

    /**
     * Gets a node by its id.
     *
     * @see #getId()
     *
     * @param id
     *            the id of the node to get
     * @return the node with the given id, or <code>null</code> if no such node
     *         exists
     */
    public static TemplateNode get(int id) {
        return registry.get(Integer.valueOf(id));
    }

    /**
     * Gets the element state provider used for elements based on this node.
     *
     * @return the element state provider, not <code>null</code>
     */
    public ElementStateProvider getStateProvider() {
        return stateProvider;
    }

    /**
     * Creates an element state provider that will be used for all elements
     * based on this node. This method is called by the super constructor, so
     * implementations should avoid accessing own internal fields from inside
     * the method.
     *
     * @return the element state provider, not <code>null</code>
     */
    protected abstract ElementStateProvider createStateProvider();
}
