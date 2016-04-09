package com.vaadin.hummingbird.dom;

import java.util.EventObject;

/**
 * Event fired after an Element is attached to the UI.
 */
public class AttachEvent extends EventObject {

    /**
     * Creates a new attach event with the given element as source.
     *
     * @param source
     *            the element that was attached
     */
    public AttachEvent(Element source) {
        super(source);
    }

    /**
     * Gets the element that was attached to the UI.
     *
     * @return the attached element
     */
    public Element getElement() {
        return (Element) getSource();
    }
}
