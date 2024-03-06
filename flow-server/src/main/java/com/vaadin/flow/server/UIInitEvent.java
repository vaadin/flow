/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.util.EventObject;

import com.vaadin.flow.component.UI;

/**
 * Event fired to {@link UIInitListener} when a {@link UI} has been initialized.
 *
 * @since 1.0
 */
public class UIInitEvent extends EventObject {

    private final UI ui;

    /**
     * Constructs a prototypical Event.
     *
     * @param service
     *            the service from which the event originates
     * @param ui
     *            the initialized UI
     */
    public UIInitEvent(UI ui, VaadinService service) {
        super(service);
        this.ui = ui;
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    /**
     * Get the initialized UI for this initialization event.
     *
     * @return initialized UI
     */
    public UI getUI() {
        return ui;
    }
}
