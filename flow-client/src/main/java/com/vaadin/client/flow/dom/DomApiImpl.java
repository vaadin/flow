/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.dom;

import elemental.dom.Node;

/**
 * A DOM API abstraction layer to be used via {@link DomApi#wrap(Node)}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface DomApiImpl {

    /**
     * Wraps the given DOM node to make it safe to invoke any of the methods
     * from {@link DomNode} or {@link DomElement}.
     *
     * @param node
     *            the node to wrap
     * @return the wrapped element
     */
    DomElement wrap(Node node);
}
