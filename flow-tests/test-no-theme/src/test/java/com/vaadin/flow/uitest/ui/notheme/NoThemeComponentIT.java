/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.notheme;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NoThemeComponentIT extends ChromeBrowserTest {

    @Test
    public void themeIsNotApplied() {
        open();

        List<WebElement> customStyles = $("head").first()
                .findElements(By.tagName("custom-style"));

        Assert.assertEquals(
                "Found custom style whereas @NoTheme should disable Lumo "
                        + "and not add any 'custom-theme' element",
                0, customStyles.size());

        String color = $("a").first().getCssValue("color");
        Assert.assertEquals(
                "Unexpected color for a link. "
                        + "@NoTheme should not theme a link anyhow.",
                "rgba(0, 0, 0, 1)", color);
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        String result;
        if (path.startsWith("/")) {
            result = path.substring(view.length() + 1);
        }
        result = path.substring(view.length());
        return result;
    }
}
