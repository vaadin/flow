/*
 * Copyright 2000-2026 Vaadin Ltd.
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
 * Per-node id allocator for JS initializers registered through
 * {@link com.vaadin.flow.dom.Element#addJsInitializer(String, Object...)}.
 * <p>
 * The counter is server-side only and is not sent to the client. The
 * {@code (nodeId, initializerId)} pair is what the client uses to key its
 * cleanup map; uniqueness within a node is enough.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class JsInitializerCounter extends ServerSideFeature {

    private int nextId = 0;

    /**
     * Creates a new initializer counter for the given state node.
     *
     * @param node
     *            the owning state node, not {@code null}
     */
    public JsInitializerCounter(StateNode node) {
        super(node);
    }

    /**
     * Returns the next unique initializer id for this node.
     *
     * @return the next id
     */
    public int next() {
        return nextId++;
    }
}
