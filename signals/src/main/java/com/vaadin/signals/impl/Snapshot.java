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
