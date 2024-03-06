/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.internal.StateNode;

/**
 * Map holding the data of a text node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TextNodeMap extends NodeValue<String> {

    /**
     * Creates a new text node map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public TextNodeMap(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.TEXT;
    }

    /**
     * Sets the text of this node.
     *
     * @param text
     *            the text, not <code>null</code>
     */
    public void setText(String text) {
        assert text != null;

        setValue(text);
    }

    /**
     * Gets the text of this node.
     *
     * @return the text, not null
     */
    public String getText() {
        String value = getValue();

        // Text should be set upon creation, before first use
        assert value != null;

        return value;
    }

}
