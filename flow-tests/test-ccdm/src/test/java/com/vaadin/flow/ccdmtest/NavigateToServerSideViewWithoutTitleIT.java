/*
 * Copyright 2000-2025 Vaadin Ltd.
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
