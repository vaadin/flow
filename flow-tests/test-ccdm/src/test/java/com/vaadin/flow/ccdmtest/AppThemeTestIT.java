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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

public class AppThemeTestIT extends CCDMTest {
    private static final String BLUE_RGBA = "rgba(0, 0, 255, 1)";
    private static final String RED_RGBA = "rgba(255, 0, 0, 1)";

    @Before
    public void setUp() {
        openVaadinRouter();
    }

    @Test
    public void should_apply_AppTheme_on_clientSideView() {
        findAnchor("client-view").click();
        Assert.assertEquals(RED_RGBA,
                findElement(By.id("clientView")).getCssValue("color"));
    }

    @Test
    public void should_apply_AppTheme_on_serverSideView() {
        findAnchor("serverview").click();
        Assert.assertEquals(BLUE_RGBA,
                findElement(By.id("serverView")).getCssValue("color"));
    }
}
