/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.component.UI;

import elemental.json.JsonValue;

/**
 * Event object with data related to error navigation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ErrorNavigationEvent extends NavigationEvent {

    private final ErrorParameter<?> errorParameter;

    /**
     * Creates a new navigation event.
     *
     * @param router
     *            the router handling the navigation, not {@code null}
     * @param location
     *            the new location, not {@code null}
     * @param ui
     *            the UI in which the navigation occurs, not {@code null}
     * @param trigger
     *            the type of user action that triggered this navigation event,
     *            not {@code null}
     * @param errorParameter
     *            parameter containing navigation error information
     */
    public ErrorNavigationEvent(Router router, Location location, UI ui,
            NavigationTrigger trigger, ErrorParameter<?> errorParameter) {
        super(router, location, ui, trigger);

        this.errorParameter = errorParameter;
    }

    /**
     * Creates a new navigation event.
     *
     * @param router
     *            the router handling the navigation, not {@code null}
     * @param location
     *            the new location, not {@code null}
     * @param ui
     *            the UI in which the navigation occurs, not {@code null}
     * @param trigger
     *            the type of user action that triggered this navigation event,
     *            not {@code null}
     * @param errorParameter
     *            parameter containing navigation error information
     * @param state
     *            includes navigation state info including for example the
     *            scroll position and the complete href of the RouterLink
     */
    public ErrorNavigationEvent(Router router, Location location, UI ui,
            NavigationTrigger trigger, ErrorParameter<?> errorParameter,
            JsonValue state) {
        super(router, location, ui, trigger, state, false);

        this.errorParameter = errorParameter;
    }

    /**
     * Gets the ErrorParameter if set.
     *
     * @return set error parameter or null if not set
     */
    public ErrorParameter<?> getErrorParameter() {
        return errorParameter;
    }
}
