/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;

import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.impl.BasicElementStyle;
import com.vaadin.flow.internal.StateNode;

/**
 * Map for element style values.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementStylePropertyMap extends AbstractPropertyMap {

    /**
     * Creates a new element style map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ElementStylePropertyMap(StateNode node) {
        super(node);
    }

    @Override
    public void setProperty(String name, Serializable value,
            boolean emitChange) {
        assert value instanceof String;
        assert ElementUtil.isValidStylePropertyValue((String) value);
        super.setProperty(name, value, emitChange);
    }

    /**
     * Returns a style instance for managing element inline styles.
     *
     * @return a Style instance connected to this map
     */
    public Style getStyle() {
        return new BasicElementStyle(this);
    }

}
