package com.vaadin.router.event;

import com.vaadin.router.ErrorParameter;
import com.vaadin.router.Location;
import com.vaadin.router.NavigationTrigger;
import com.vaadin.router.RouterInterface;
import com.vaadin.ui.UI;

public class ErrorNavigationEvent extends NavigationEvent {

    private final ErrorParameter errorParameter;

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
    public ErrorNavigationEvent(RouterInterface router, Location location,
            UI ui, NavigationTrigger trigger, ErrorParameter errorParameter) {
        super(router, location, ui, trigger);

        this.errorParameter = errorParameter;
    }

    /**
     * Gets the ErrorParameter if set.
     *
     * @return set error parameter or null if not set
     */
    public ErrorParameter getErrorParameter() {
        return errorParameter;
    }
}
