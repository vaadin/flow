/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import java.util.function.Supplier;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ServerSidePostponeIT extends CCDMTest {

    private static final String SERVER_POSTPONE_VIEW_ROUTE = "serverpostponeview";

    private static final By PROCEED_BUTTON = By.id("proceedAfterPostpone");

    @Test
    public void should_preserveUrlAndView_when_navigateToServerViewAndPostpone() {
        openVaadinRouter();

        assertPostponeNavigationFromButton("serverview", "serverView",
                "Server view", "goToServerView");

        assertPostponeNavigationFromRouterLink("serverview", "serverView",
                "Server view", "RouterLink Server View");

        assertPostponeNavigationFromHref("serverview", "serverView",
                "Server view");
    }

    @Test
    public void should_preserveUrlAndView_when_navigateToServerViewAndPostponeMultipleTimes() {
        openVaadinRouter();

        // Sequentially try to leave current page to different urls until only
        // the last one proceeds.
        assertPostponeNavigation("serverview", "serverView", "Server view",
                () -> findElement(By.id("goToServerView")), // server button
                () -> findAnchor("client-view"), // client link
                () -> findAnchor("serverpostponeview"), // same server page link
                () -> findAnchor("serverview"), // server link
                () -> findElement(By.id("goToClientView")), // client button
                () -> findAnchor("serverview") // server link
        );
    }

    @Test
    public void should_preserveUrlAndView_when_navigateToClientViewAndPostpone() {
        openVaadinRouter();

        assertPostponeNavigationFromButton("client-view", "clientView",
                "Client view", "goToClientView");

        assertPostponeNavigationFromHref("client-view", "clientView",
                "Client view");
    }

    private void assertPostponeNavigationFromButton(String destinationViewRoute,
            String destinationViewDiv, String destinationViewText,
            String goToDestinationButtonId) {
        assertPostponeNavigation(destinationViewRoute, destinationViewDiv,
                destinationViewText,
                () -> findElement(By.id(goToDestinationButtonId)));
    }

    private void assertPostponeNavigationFromRouterLink(
            String destinationViewRoute, String destinationViewDiv,
            String destinationViewText, String goToDestinationRouterLinkText) {
        assertPostponeNavigation(destinationViewRoute, destinationViewDiv,
                destinationViewText, () -> {
                    final WebElement routerLinkElement = findElement(
                            By.linkText(goToDestinationRouterLinkText));
                    return routerLinkElement.getAttribute("router-link") != null
                            ? routerLinkElement
                            : null;
                });
    }

    private void assertPostponeNavigationFromHref(String goToDestinationHref,
            String destinationViewDiv, String destinationViewText) {
        assertPostponeNavigation(goToDestinationHref, destinationViewDiv,
                destinationViewText, () -> findAnchor(goToDestinationHref));
    }

    private void assertPostponeNavigation(String destinationViewRoute,
            String destinationViewDiv, String destinationViewText,
            Supplier<WebElement>... destinationElements) {

        findAnchor(SERVER_POSTPONE_VIEW_ROUTE).click();
        waitForElementPresent(By.id("serverPostponeView"));

        for (Supplier<WebElement> destinationElement : destinationElements) {
            destinationElement.get().click();

            waitForElementPresent(PROCEED_BUTTON);

            assertCurrentRoute(SERVER_POSTPONE_VIEW_ROUTE);
        }

        findElement(PROCEED_BUTTON).click();

        assertView(destinationViewDiv, destinationViewText,
                destinationViewRoute);
    }

}
