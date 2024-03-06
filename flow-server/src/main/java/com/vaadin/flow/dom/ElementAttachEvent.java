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
 * Event fired after an Element has been attached to the UI.
 * <p>
 * When a hierarchy of elements is being attached, this event is fired
 * child-first.
 *
 * @since 1.0
 */
public class ElementAttachEvent extends EventObject {

    /**
     * Creates a new attach event with the given element as source.
     *
     * @param source
     *            the element that was attached
     */
    public ElementAttachEvent(Element source) {
        super(source);
    }

    @Override
    public Element getSource() {
        return (Element) super.getSource();
    }
}
