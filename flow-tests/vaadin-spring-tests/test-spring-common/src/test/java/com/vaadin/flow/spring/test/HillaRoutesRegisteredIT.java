/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class HillaRoutesRegisteredIT extends AbstractSpringTest {

    @Test
    public void assertClientRoutesRegistered() {
        String nonExistingRoutePath = "non-existing-route";
        getDriver().get(getContextRootURL() + '/' + nonExistingRoutePath);
        waitForDevServer();
        Assert.assertTrue(getDriver().getPageSource().contains(String
                .format("Could not navigate to '%s'", nonExistingRoutePath)));

        if (getDriver().getPageSource().contains(
                "This detailed message is only shown when running in development mode.")) {
            var expectedClientRoutes = List.of("<a href=\"\">&lt;root&gt;</a>",
                    "<a href=\"/hilla\">hilla</a>",
                    "<li>hilla/person/:id (requires parameter)</li>",
                    "<li>hilla/persons/:id? (supports optional parameter)</li>",
                    "<a href=\"/hilla/hilla/\">hilla/hilla/</a>",
                    "<a href=\"/anotherhilla\">anotherhilla</a>");
            for (String route : expectedClientRoutes) {
                Assert.assertTrue(
                        String.format("Expected client route %s is missing",
                                route),
                        getDriver().getPageSource().contains(route));
            }
        }
    }
}
