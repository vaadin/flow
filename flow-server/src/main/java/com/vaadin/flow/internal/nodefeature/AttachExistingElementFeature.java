/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.StateNode;

/**
 * Temporary storage of data required to handle existing element attachment
 * callback from the client side.
 * <p>
 * The data is going to be destroyed once the response from the client side is
 * received.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class AttachExistingElementFeature extends ServerSideFeature {

    private Map<StateNode, ChildElementConsumer> callbacks;
    private Map<StateNode, Node<?>> parentNodes;
    private Map<StateNode, Element> siblings;

    /**
     * Creates a new instance for the given node.
     *
     * @param node
     *            the node that the feature belongs to
     */
    public AttachExistingElementFeature(StateNode node) {
        super(node);
    }

    /**
     * Registers the data for the {@code child} node requested as being attached
     * to an existing element.
     *
     * @param parent
     *            parent node of the {@code child}
     * @param previousSibling
     *            previous sibling for the requested existing element
     * @param child
     *            the state node that is going to be associated with the
     *            existing element
     * @param callback
     *            the callback to report the result
     */
    public void register(Node<?> parent, Element previousSibling,
            StateNode child, ChildElementConsumer callback) {
        if (callbacks == null) {
            callbacks = new HashMap<>();
        }
        if (parentNodes == null) {
            parentNodes = new HashMap<>();
        }
        if (siblings == null) {
            siblings = new HashMap<>();
        }
        callbacks.put(child, callback);
        parentNodes.put(child, parent);
        siblings.put(child, previousSibling);
        child.setParent(getNode());
    }

    /**
     * Gets callback of the registered {@code node}.
     *
     * @param node
     *            the registered state node
     * @return the registered callback for the {@code node}
     */
    public ChildElementConsumer getCallback(StateNode node) {
        return callbacks.remove(node);
    }

    /**
     * Gets parent {@link Node} of the registered {@code node}.
     *
     * @param node
     *            the registered state node
     * @return the registered parent for the {@code node}
     */
    public Node<?> getParent(StateNode node) {
        return parentNodes.get(node);
    }

    /**
     * Gets previous sibling of the registered {@code node}.
     *
     * @param node
     *            the registered state node
     * @return the registered previous sibling for the {@code node}
     */
    public Element getPreviousSibling(StateNode node) {
        return siblings.get(node);
    }

    /**
     * Unregister the {@code node} and clean up all associated data.
     *
     * @param node
     *            the registered state node
     */
    public void unregister(StateNode node) {
        callbacks.remove(node);
        parentNodes.remove(node);
        siblings.remove(node);
        node.setParent(null);
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        if (callbacks != null) {
            callbacks.keySet().forEach(action::accept);
        }
    }

}
