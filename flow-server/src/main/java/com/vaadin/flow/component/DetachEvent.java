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
 * Event fired before a {@link Component} is detached from the UI.
 * <p>
 * When a hierarchy of components is being detached, this event is fired
 * child-first.
 *
 * @since 1.0
 */
public class DetachEvent extends AbstractAttachDetachEvent {

    /**
     * Creates a new detach event with the given component as source.
     *
     * @param source
     *            the component that was detached
     */
    public DetachEvent(Component source) {
        super(source);
    }

}
