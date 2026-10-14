/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

import java.io.Serializable;

/**
 * Interface for controlling access to routes in the application's menu
 * component.
 */
public interface MenuAccessControl extends Serializable {

    /**
     * Enum for controlling how the client-side menu should be populated.
     */
    enum PopulateClientMenu {
        /**
         * Always populate the menu with server side routes.
         */
        ALWAYS,
        /**
         * Never populate the menu with server side routes.
         */
        NEVER,
        /**
         * Populate the menu with server side routes only if client-side menu
         * exists.
         */
        AUTOMATIC
    };

    /**
     * Sets whether the Hilla application's main menu should be populated
     * automatically with server side routes and therefore routes information
     * sent to the browser. Three possible values:
     * {@link PopulateClientMenu#ALWAYS}, {@link PopulateClientMenu#NEVER},
     * {@link PopulateClientMenu#AUTOMATIC}.
     *
     * @param populateClientSideMenu
     *            whether the client-side menu should be populated with server
     *            side routes
     */
    void setPopulateClientSideMenu(PopulateClientMenu populateClientSideMenu);

    /**
     * Gets whether the Hilla application's main menu should be populated
     * automatically with server side routes and therefore routes information
     * sent to the browser.
     *
     * @return enum of type {@link PopulateClientMenu}
     */
    PopulateClientMenu getPopulateClientSideMenu();

}
