/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

/**
 * Default implementation of {@link MenuAccessControl}.
 *
 * @since 24.4
 */
public class DefaultMenuAccessControl implements MenuAccessControl {

    private PopulateClientMenu populateClientSideMenu = PopulateClientMenu.AUTOMATIC;

    @Override
    public void setPopulateClientSideMenu(
            PopulateClientMenu populateClientSideMenu) {
        this.populateClientSideMenu = populateClientSideMenu;
    }

    @Override
    public PopulateClientMenu getPopulateClientSideMenu() {
        return populateClientSideMenu;
    }
}
