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
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ChangeVisitor;

/**
 * A namespace represents a group of related values in a state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class Namespace
        implements Serializable, Consumer<ChangeVisitor> {
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
     * Collects all changes made to this namespace since the last time
     * {@link #accept(ChangeVisitor)} or {@link #resetChanges()} has been
     * called.
     *
     * @param visitor
     *            a visitor visiting node changes
     */
    @Override
    public abstract void accept(ChangeVisitor visitor);

    /**
     * Callback method is called when the underlying node becomes attached.
     */
    public void nodeAttached() {
        resetChanges();
    }

    /**
     * Resets the collected changes of this namespace so that the next
     * invocation of {@link #accept(ChangeVisitor)} will report changes relative
     * to a newly created namespace.
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

    /**
     * Passes each child node instance to the given consumer.
     * 
     * @param action
     *            the consumer that accepts each child
     */
    public abstract void forEachChild(Consumer<StateNode> action);
}
