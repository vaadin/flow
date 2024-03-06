/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.component.internal.AbstractAttachDetachEvent;

/**
 * Event fired after a {@link Component} is attached to the UI.
 * <p>
 * When a hierarchy of components is being attached, this event is fired
 * child-first.
 *
 * @since 1.0
 */
public class AttachEvent extends AbstractAttachDetachEvent {

    private boolean initialAttach;

    /**
     * Creates a new attach event with the given component as source.
     *
     * @param source
     *            the component that was attached
     * @param initialAttach
     *            indicates whether this is the first time the component
     *            (element) has been attached
     */
    public AttachEvent(Component source, boolean initialAttach) {
        super(source);
        this.initialAttach = initialAttach;
    }

    /**
     * Checks whether this is the first time the component has been attached.
     *
     * @return <code>true</code> if this it the first time the component has
     *         been attached, <code>false</code> otherwise
     */
    public boolean isInitialAttach() {
        return initialAttach;
    }

}
