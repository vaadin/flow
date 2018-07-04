/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.internal;

import java.io.Serializable;

/**
 * Abstract root of a state node tree. A node always belongs to one specific
 * owner. The owner keeps track of metadata for its nodes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface NodeOwner extends Serializable {

    /**
     * Registers a node with this node owner. The new node should already be set
     * to be owned by this instance.
     *
     * @param node
     *            the node to register
     * @return the id of the registered node
     */
    int register(StateNode node);

    /**
     * Unregisters a node from this owner. This must be done before the node is
     * set to not be owned by this instance.
     *
     * @param node
     *            the node to unregister
     */
    void unregister(StateNode node);

    /**
     * Marks a node owned by this instance as dirty. Dirty nodes are collected
     * from an owner using {@link StateTree#collectDirtyNodes()}.
     *
     * @param node
     *            the node to be marked as dirty
     */
    void markAsDirty(StateNode node);

    /**
     * Check if given node is registered to this node owner.
     * 
     * @param node
     *            node to check registration status for
     * @return true if node is registered
     */
    boolean hasNode(StateNode node);
}
