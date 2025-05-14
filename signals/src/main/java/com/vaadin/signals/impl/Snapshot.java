/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals.impl;

import java.util.Map;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node;

/**
 * An immutable tree revision.
 */
public class Snapshot extends TreeRevision {
    /**
     * Creates a new snapshot from a mutable tree revision.
     *
     * @param base
     *            the mutable base revision to copy, not <code>null</code>
     */
    public Snapshot(MutableTreeRevision base) {
        super(base.ownerId(), Map.copyOf(base.nodes()),
                Map.copyOf(base.originalInserts()));
    }

    /**
     * Creates an empty snapshot. The snapshot contains an empty root node with
     * {@link Id#ZERO} that is used for tracking signal values and optionally
     * also another empty root node with {@link Id#MAX} that is used for
     * tracking metadata.
     *
     * @param ownerId
     *            the id of the tree owner, not <code>null</code>
     * @param includeMax
     *            flag indicating whether an additional root node should be
     *            created for tracking metadata
     */
    public Snapshot(Id ownerId, boolean includeMax) {
        super(ownerId,
                includeMax ? Map.of(Id.ZERO, Node.EMPTY, Id.MAX, Node.EMPTY)
                        : Map.of(Id.ZERO, Node.EMPTY),
                Map.of());
    }
}
