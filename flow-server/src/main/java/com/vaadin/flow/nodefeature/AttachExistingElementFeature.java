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
package com.vaadin.flow.nodefeature;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.ChildElementConsumer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;

/**
 * Temporary storage of data required to handle existing element attachment
 * callback from the client side.
 * <p>
 * The data is going to be destroyed once the response from the client side is
 * received.
 * 
 * @author Vaadin Ltd
 *
 */
public class AttachExistingElementFeature extends ServerSideFeature {

    private Map<StateNode, ChildElementConsumer> callbacks = new HashMap<>();
    private Map<StateNode, Node<?>> parentNodes = new HashMap<>();
    private Map<StateNode, Element> siblings = new HashMap<>();

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
        callbacks.keySet().stream().forEach(action::accept);
    }

}
