/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;

/**
 * Handles navigation by redirecting the user to some location in the
 * application.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class InternalRedirectHandler implements NavigationHandler {
    private final Location target;

    /**
     * Creates a new redirect handler for the provided location.
     *
     * @param target
     *            the target of the redirect, not <code>null</code>
     */
    public InternalRedirectHandler(Location target) {
        assert target != null;
        this.target = target;
    }

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();
        Router router = event.getSource();

        if (NavigationTrigger.PAGE_LOAD.equals(event.getTrigger())) {
            ui.getPage().getHistory().replaceState(null, target);
        }

        return router.navigate(ui, target, event.getTrigger(),
                event.getState().orElse(null));
    }
}
