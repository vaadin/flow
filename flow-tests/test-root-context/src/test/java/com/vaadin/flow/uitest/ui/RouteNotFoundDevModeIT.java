/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.testcategory.IgnoreOSGi;

@Category(IgnoreOSGi.class)
public class RouteNotFoundDevModeIT extends RouteNotFoundIT {

    @Test
    public void notFoundDevMode() {
        getDriver().get(getRootURL() + "/view/notfound/" + INJECT_ATTACK);
        waitForDevServer();
        assertPageHasRoutes(true);
    }
}
