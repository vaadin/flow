/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.ccdmtest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ServerSidePostponeIT extends CCDMTest {

    private static final String SERVER_POSTPONE_VIEW_ROUTE = "serverpostponeview";

    private static final By PROCEED_BUTTON = By.id("proceedAfterPostpone");

    @Test
    public void should_preserveUrlAndView_when_navigateToServerViewAndPostpone() {
        openVaadinRouter();

        assertPostponeNavigation("goToServerView", "serverview", "serverView",
                "Server view");
    }

    @Test
    public void should_preserveUrlAndView_when_navigateToClientViewAndPostpone() {
        openVaadinRouter();

        assertPostponeNavigation("goToClientView", "client-view", "clientView",
                "Client view");
    }

    private void assertPostponeNavigation(String goToDestinationViewId,
            String destinationViewRoute, String destinationViewDiv,
            String destinationViewText) {
        findAnchor(SERVER_POSTPONE_VIEW_ROUTE).click();

        waitForElementPresent(By.id("serverPostponeView"));

        findElement(By.id(goToDestinationViewId)).click();

        waitForElementPresent(PROCEED_BUTTON);

        assertCurrentRoute(SERVER_POSTPONE_VIEW_ROUTE);

        findElement(PROCEED_BUTTON).click();

        waitForElementPresent(By.id(destinationViewDiv));
        final WebElement serverViewDiv = findElement(By.id(destinationViewDiv));

        Assert.assertEquals(destinationViewText, serverViewDiv.getText());
        assertCurrentRoute(destinationViewRoute);
    }

    private void assertCurrentRoute(String route) {
        final String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(String.format("Expecting route '%s', but url is '%s'",
                route, currentUrl), currentUrl.endsWith(route));
    }

}
