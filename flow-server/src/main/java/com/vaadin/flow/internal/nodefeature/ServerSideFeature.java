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
package com.vaadin.flow.internal.nodefeature;

import java.util.function.Consumer;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;

/**
 * Abstract node feature that is only present on the server. A server side
 * feature does not produce any node changes and it can't contain child nodes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class ServerSideFeature extends NodeFeature {

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public ServerSideFeature(StateNode node) {
        super(node);
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        // Server side only feature
    }

    @Override
    public void generateChangesFromEmpty() {
        // Server side only feature
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        // Server side only feature -> can't have child nodes
    }
}
