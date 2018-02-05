package com.vaadin.flow.router.internal;

import com.vaadin.flow.router.*;

/**
 * base-interface for all interfaces handling the {@link BeforeEnterEvent}.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface BeforeEnterHandler {

    /**
     * Method called before navigation.
     *
     * @param event
     *            before navigation event with event details
     */
    void beforeEnter(BeforeEnterEvent event);
}
