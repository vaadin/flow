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

package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;

/**
 * A namespace represents a group of related values in a state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class Namespace implements Serializable {
    private static int nextNamespaceId = 0;

    // Non-private for testing purposes
    static final Map<Class<? extends Namespace>, NamespaceData> namespaces = new HashMap<>();

    private static class NamespaceData implements Serializable {
        private Function<StateNode, ? extends Namespace> factory;
        private int id = nextNamespaceId++;

        public <T extends Namespace> NamespaceData(
                Function<StateNode, T> factory) {
            this.factory = factory;
        }
    }

    private static <T extends Namespace> void registerNamespace(Class<T> type,
            Function<StateNode, T> factory) {
        namespaces.put(type, new NamespaceData(factory));
    }

    static {
        registerNamespace(ElementDataNamespace.class,
                ElementDataNamespace::new);
        registerNamespace(ElementPropertiesNamespace.class,
                ElementPropertiesNamespace::new);
        registerNamespace(ElementChildrenNamespace.class,
                ElementChildrenNamespace::new);
    }

    private final StateNode node;

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public Namespace(StateNode node) {
        this.node = node;
    }

    /**
     * Gets the node that this namespace belongs to.
     *
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Creates a namespace of the given type for a node.
     *
     * @param namespaceType
     *            the type of the namespace to create
     * @param node
     *            the node for which the namespace should be created
     * @return a newly created namespace
     */
    public static Namespace create(Class<? extends Namespace> namespaceType,
            StateNode node) {
        assert node != null;

        return getData(namespaceType).factory.apply(node);
    }

    /**
     * Collects all changes made to this namespace since the last time
     * {@link #collectChanges(Consumer)} or {@link #resetChanges()} has been
     * called.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public abstract void collectChanges(Consumer<NodeChange> collector);

    /**
     * Resets the collected changes of this namespace so that the next
     * invocation of {@link #collectChanges(Consumer)} will report changes
     * relative to a newly created namespace.
     */
    public abstract void resetChanges();

    /**
     * Attaches an object if it is a {@link StateNode}.
     *
     * @param child
     *            the instance to maybe attach
     */
    protected void attachPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;
            childNode.setParent(getNode());
        }
    }

    /**
     * Detaches an object if it is a {@link StateNode}.
     *
     * @param child
     *            the instance to maybe detach
     */
    @SuppressWarnings("static-method")
    protected void detatchPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;
            childNode.setParent(null);
        }
    }

    /**
     * Gets the id of a namespace type.
     *
     * @param namespace
     *            the namespace type
     * @return the id of the namespace type
     */
    public static int getId(Class<? extends Namespace> namespace) {
        return getData(namespace).id;
    }

    private static NamespaceData getData(Class<? extends Namespace> namespace) {
        assert namespace != null;

        NamespaceData data = namespaces.get(namespace);

        assert data != null;

        return data;
    }
}
