/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PreserveOnRefreshForwardingIT extends ChromeBrowserTest {

    @Test
    public void forwadingToPreserveOnRefreshRoute_allowsUpdatingQueryParameters() {
        open("initial");

        waitUntil(driver -> {
            String url = driver.getCurrentUrl();
            return url.endsWith(
                    "PreserveOnRefreshForwardToView?afterforward=true");
        });
    }
}
