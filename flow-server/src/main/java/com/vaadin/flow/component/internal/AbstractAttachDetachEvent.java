/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.internal;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Internal helper for {@link AttachEvent} and {@link DetachEvent}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractAttachDetachEvent
        extends ComponentEvent<Component> {

    /**
     * Creates a new event with the given component as source.
     *
     * @param source
     *            the component that was attached or detached
     */
    public AbstractAttachDetachEvent(Component source) {
        super(source, false);
    }

    /**
     * Gets the UI the component is attached to.
     *
     * @return the UI this component is attached to
     */
    public UI getUI() {
        return getSource().getUI().get();
    }

    /**
     * Gets the session the component is attached to.
     *
     * @return the session this component is attached to
     */
    public VaadinSession getSession() {
        return getUI().getSession();
    }

}
