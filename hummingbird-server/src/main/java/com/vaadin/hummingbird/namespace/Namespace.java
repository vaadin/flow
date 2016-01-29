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
    private static Map<Class<? extends Namespace>, Function<StateNode, Namespace>> namespaceFactoies = new HashMap<>();

    static {
        namespaceFactoies.put(ElementDataNamespace.class,
                ElementDataNamespace::new);
        namespaceFactoies.put(ElementPropertiesNamespace.class,
                ElementPropertiesNamespace::new);
        namespaceFactoies.put(ElementChildrenNamespace.class,
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
        assert namespaceType != null;
        assert node != null;

        Function<StateNode, Namespace> factory = namespaceFactoies
                .get(namespaceType);
        assert factory != null;

        return factory.apply(node);
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
    protected void detatchPotentialChild(Object child) {
        if (child instanceof StateNode) {
            StateNode childNode = (StateNode) child;

            // Should always be parent of our own children
            assert childNode.getParent() == getNode();

            childNode.setParent(null);
        }
    }
}
