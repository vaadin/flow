/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class HtmlParserThemeTemplateIT extends ChromeBrowserTest {

    @Test
    public void themeComponentShouldLoadForTemplate() {
        getDriver().get(getRootURL()
                + "/view/com.vaadin.flow.uitest.ui.theme.HtmlParserThemeTemplateView");

        Assert.assertTrue(findElement(By.id("theme-component")).isDisplayed());
    }

}
