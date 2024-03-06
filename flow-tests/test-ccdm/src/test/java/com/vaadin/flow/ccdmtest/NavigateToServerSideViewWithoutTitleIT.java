/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class NavigateToServerSideViewWithoutTitleIT extends CCDMTest {

    @Test
    public void should_showAppShellTitle_when_navigateToAServerSideViewWithoutTitle() {
        openVaadinRouter();

        findAnchor("view-with-server-view-button").click();

        // "app-shell-title" is defined in AppShell
        Assert.assertEquals("Shoul use app shell title", "app-shell-title",
                getDriver().getTitle());
    }

    @Test
    public void should_showAppShellTitle_after_titleHasBeenChangedOnTheClientSide_when_navigateToAServerSideViewWithoutTitle() {
        openVaadinRouter();

        // update document.title on the client-side
        findElement(By.id("updatePageTitle")).click();
        Assert.assertEquals(
                "Client-side should have updated the app shell title",
                "client-side-updated-title", getDriver().getTitle());

        findAnchor("view-with-server-view-button").click();

        // "app-shell-title" is defined in AppShell
        Assert.assertEquals("Shoul use app shell title", "app-shell-title",
                getDriver().getTitle());
    }

}
