/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("disabled, because production mode can't be enabled via init servlet parameter, see https://github.com/vaadin/flow/issues/7281")
public class RouteNotFoundProdModeIT extends RouteNotFoundIT {

    @Test
    public void notFoundProdMode() {
        getDriver().get(
                getRootURL() + "/view-production/notfound/" + INJECT_ATTACK);
        waitForDevServer();
        assertPageHasRoutes(false);
    }

}
