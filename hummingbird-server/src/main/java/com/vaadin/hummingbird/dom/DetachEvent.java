package com.vaadin.hummingbird.dom;

import java.util.EventObject;

/**
 * Event fired after an element is detached from the UI.
 */
public class DetachEvent extends EventObject {

    /**
     * Creates a new detach event with the given element as source.
     *
     * @param source
     *            the element that was detached
     */
    public DetachEvent(Element source) {
        super(source);
    }

    /**
     * Gets the element that was detached from the UI.
     *
     * @return the detached element
     */
    public Element getElement() {
        return (Element) getSource();
    }
}
