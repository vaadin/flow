package com.vaadin.flow.router.internal;

import com.vaadin.flow.router.*;

/**
 * base-interface for all interfaces handling the {@link BeforeLeaveEvent}.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface BeforeLeaveHandler {
    void beforeLeave(BeforeLeaveEvent event);
}
