/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NPEHandlerIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/npe";
    }

    @Test
    public void npeIsHandledByComponent() {
        open();

        Assert.assertTrue(
                "Couldn't find element from the component which represents error for "
                        + NullPointerException.class.getName(),
                isElementPresent(By.id("npe-handle")));
    }

    @Test
    public void noRouteIsHandledByExistingFlowComponent() {
        String nonExistingRoutePath = "non-existing-route";
        getDriver().get(getTestURL(getRootURL(), '/' + nonExistingRoutePath,
                new String[0]));

        Assert.assertTrue(getDriver().getPageSource().contains(String
                .format("Could not navigate to '%s'", nonExistingRoutePath)));
        Assert.assertTrue(getDriver().getPageSource().contains(String.format(
                "Reason: Couldn't find route for '%s'", nonExistingRoutePath)));
    }
}
