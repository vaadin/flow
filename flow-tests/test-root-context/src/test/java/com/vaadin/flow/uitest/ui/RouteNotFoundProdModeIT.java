/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;

public class RouteNotFoundProdModeIT extends RouteNotFoundIT {

    @Test
    public void notFoundProdMode() {
        getDriver().get(
                getRootURL() + "/view-production/notfound/" + INJECT_ATTACK);
        waitForDevServer();
        assertPageHasRoutes(false);
    }

}
