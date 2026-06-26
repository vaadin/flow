/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.auth;

import java.util.Optional;

public class CustomMenuAccessControl implements MenuAccessControl {

    @Override
    public void setPopulateClientSideMenu(
            PopulateClientMenu populateClientSideMenu) {
    }

    @Override
    public PopulateClientMenu getPopulateClientSideMenu() {
        return PopulateClientMenu.ALWAYS;
    }
}
