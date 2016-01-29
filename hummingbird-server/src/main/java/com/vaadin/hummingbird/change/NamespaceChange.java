package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.Namespace;

/**
 * Base class for all node changes related to a namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class NamespaceChange extends NodeChange {

    private final Class<? extends Namespace> namespace;

    /**
     * Creates a new change for the given namespace.
     *
     * @param namespace
     *            the namespace affected by the change
     */
    public NamespaceChange(Namespace namespace) {
        super(namespace.getNode());

        this.namespace = namespace.getClass();
    }

    /**
     * Gets the namespace affected by the change.
     *
     * @return the namespace
     */
    public Class<? extends Namespace> getNamespace() {
        return namespace;
    }

}
