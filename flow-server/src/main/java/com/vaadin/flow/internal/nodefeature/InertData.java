/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.internal.nodefeature;

import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.shared.util.UniqueSerializable;

/**
 * Server side only feature about whether or not the node is inert or if it
 * should ignore inhering inert state from parent. By default the node is not
 * inert, and it will inherit the inert state from the parent. If the node lacks
 * the inert data, then it will be just inheriting the state from parent.
 * <p>
 * The inert status is only updated when the changes are written to the client
 * side because the inert state changes are applied for upcoming requests from
 * the client side. Thus when an RPC call (like any DOM event) causes a node to
 * become inert, the state is does not block any pending executions until
 * changes are written to the client side.
 * <p>
 * Implementation notes: The inert state changes are collected like with client
 * side changes (markAsDirty), but nothing is actually sent to the client side.
 * This is just to make sure the changes are applied when needed, when writing
 * changes to client side, instead of applying them immediately. By default the
 * elements only have the inert data feature but as "not initialized" state
 * which means that the node is not inert unless parent is inert, and thus it
 * does not ignore parent inert by default. The inert data feature is
 * initialized when the node will be made explicitly inert or to explicitly
 * ignore parent inert data.
 */
public class InertData extends ServerSideFeature {
    // Null is ignored by Map.computeIfAbsent -> using a marker value instead
    private static final UniqueSerializable NULL_MARKER = new UniqueSerializable() {
        // empty
    };

    private boolean ignoreParentInert;
    private boolean inertSelf;

    /*
     * This value stores the latest inert status that the node had before the
     * latest response was sent to the client side. Otherwise any RPC handler
     * code that changes the inert state for a node in a request could cause
     * unwanted RPC handler executions to occur.
     */
    private Boolean cachedInert;

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public InertData(StateNode node) {
        super(node);
    }

    /**
     * Sets whether or not the node should ignore parent's inert state or not.
     * By default the parent state is inherited {@code false}.
     * 
     * @param ignoreParentInert
     *            {@code true} for ignoring {@code false} for not
     */
    public void setIgnoreParentInert(boolean ignoreParentInert) {
        if (this.ignoreParentInert != ignoreParentInert) {
            this.ignoreParentInert = ignoreParentInert;
            markAsDirty();
        }
    }

    /**
     * Sets whether or not the node itself is inert. By default the node is not
     * inert, unless parent is inert and inhering parent inert is not blocked.
     * 
     * @param inertSelf
     *            {@code} true for setting the node explicitly inert,
     *            {@code false} for not
     */
    public void setInertSelf(boolean inertSelf) {
        if (this.inertSelf != inertSelf) {
            this.inertSelf = inertSelf;
            markAsDirty();
        }
    }

    public boolean isInertSelf() {
        return inertSelf;
    }

    public boolean isIgnoreParentInert() {
        return ignoreParentInert;
    }

    @Override
    public void generateChangesFromEmpty() {
        updateInertAndCascadeToChildren(null);
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        updateInertAndCascadeToChildren(null);
    }

    private void markAsDirty() {
        /*
         * Even though not sending any changes to client, making sure the inert
         * status is updated for the node before writing the response by using
         * the same mechanism as collecting changes to the client.
         */
        getNode().markAsDirty();
        getNode().getChangeTracker(this, () -> NULL_MARKER);
    }

    /**
     * Returns whether this node is explicitly inert and if not, then checks
     * parents for the same. The returned value has been updated when the most
     * recent changes have been written to the client side.
     * 
     * @return {@code true} for inert, {@code false} for not
     */
    public boolean isInert() {
        if (cachedInert == null) {
            final StateNode parent = getNode().getParent();
            return parent != null && parent.isInert();
        } else {
            return cachedInert;
        }
    }

    private void updateInertAndCascadeToChildren(Boolean resolvedParentInert) {
        boolean newInert = resolveInert(resolvedParentInert);
        if (cachedInert == null || cachedInert != newInert) {
            // cascade update to all children unless those are ignoring parent
            // value or have same value and thus don't need updating.
            // (all explicitly updated nodes are visited separately)
            LinkedList<StateNode> stack = new LinkedList<>();
            getNode().forEachChild(stack::add);

            while (!stack.isEmpty()) {
                StateNode node = stack.removeFirst();

                if (node.hasFeature(InertData.class)) {
                    node.getFeatureIfInitialized(InertData.class)
                            .ifPresent(inertData -> inertData
                                    .updateInertAndCascadeToChildren(newInert));
                } else {
                    node.forEachChild(stack::addFirst);
                }
            }
        }
        cachedInert = newInert;
    }

    private boolean resolveInert(Boolean resolvedParentInert) {
        StateNode parent = getNode().getParent();
        if (inertSelf || ignoreParentInert || parent == null) {
            return inertSelf;
        }
        if (resolvedParentInert != null) {
            return resolvedParentInert;
        }
        do {
            final Optional<InertData> optionalInertData = parent
                    .getFeatureIfInitialized(InertData.class);
            if (optionalInertData.isPresent()) {
                // Most state nodes will not have inert data so using recursion
                // is safe. Need to use resolveInert() as the execution order of
                // change collection is random
                return optionalInertData
                        .map(inertData -> inertData.resolveInert(null)).get();
            } else {
                parent = parent.getParent();
            }
        } while (parent != null);
        return false;
    }
    /*
     * Not overriding allowChanges() since that is tied to isInactive() in state
     * node, which is always inherited from parent (this is maybe inherited).
     */
}
