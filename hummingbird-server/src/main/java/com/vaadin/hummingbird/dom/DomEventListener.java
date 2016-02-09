package com.vaadin.hummingbird.dom;

import java.io.Serializable;

/**
 * Server-side listener for client-side DOM events.
 *
 * @since
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface DomEventListener extends Serializable {
    /**
     * Invoked when a DOM event has been fired.
     */
    public void handleEvent();
}
