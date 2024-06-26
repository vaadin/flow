/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;

public class RouteNotFoundDevModeIT extends RouteNotFoundIT {

    @Test
    public void notFoundDevMode() {
        getDriver().get(getRootURL() + "/view/notfound/" + INJECT_ATTACK);
        waitForDevServer();
        assertPageHasRoutes(true);
    }
}
