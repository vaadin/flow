/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.util.EventObject;

/**
 * Event fired after an element has been detached from the UI.
 * <p>
 * When a hierarchy of elements is being detached, this event is fired
 * child-first.
 *
 * @since 1.0
 */
public class ElementDetachEvent extends EventObject {

    /**
     * Creates a new detach event with the given element as source.
     *
     * @param source
     *            the element that was detached
     */
    public ElementDetachEvent(Element source) {
        super(source);
    }

    @Override
    public Element getSource() {
        return (Element) super.getSource();
    }
}
