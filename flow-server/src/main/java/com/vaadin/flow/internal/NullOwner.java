/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

/**
 * A stateless singleton node owner that is used for nodes that have not yet
 * been attached to a state tree. An instance of this type is used instead of a
 * <code>null</code> pointer to avoid cluttering implementations with null
 * checks.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NullOwner implements NodeOwner {

    private static final NullOwner INSTANCE = new NullOwner();

    private NullOwner() {
        // Singleton
    }

    /**
     * Gets the singleton null owner instance.
     *
     * @return the singleton instance
     */
    public static NullOwner get() {
        return INSTANCE;
    }

    @Override
    public int register(StateNode node) {
        assert node.getOwner() == this;

        return -1;
    }

    @Override
    public void unregister(StateNode node) {
        assert node.getOwner() == this;
    }

    @Override
    public void markAsDirty(StateNode node) {
        assert node.getOwner() == this;
    }

    @Override
    public boolean hasNode(StateNode node) {
        assert node.getOwner() == this;
        return true;
    }
}
