/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
            var expectedClientRoutes = List.of("root", "/hilla",
                    "/hilla/person/:id (requires parameter)",
                    "/hilla/persons/:id? (supports optional parameter)",
                    "/hilla/hilla", "/anotherhilla", "/");
            for (String route : expectedClientRoutes) {
                Assert.assertTrue(
                        String.format("Expected client route %s is missing",
                                route),
                        getDriver().getPageSource().contains(route));
            }
        }
    }
}
