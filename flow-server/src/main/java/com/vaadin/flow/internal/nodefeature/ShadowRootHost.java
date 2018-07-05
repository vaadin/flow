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

import com.vaadin.flow.internal.StateNode;

/**
 * Marker feature for a {@link StateNode} which is a shadow root for some
 * elemement.
 * 
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ShadowRootHost extends ServerSideFeature {

    /**
     * Creates a new instance of the feature for the given {@code node}.
     * 
     * @param node
     *            the node to create the feature for
     */
    public ShadowRootHost(StateNode node) {
        super(node);
    }

    /**
     * Gets the host state node of the shadow root node.
     * 
     * @return the host element node
     */
    public StateNode getHost() {
        return getNode().getParent();
    }
}
