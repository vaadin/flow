/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class InfoIT extends ChromeBrowserTest {

    @Test
    @Ignore
    public void productionModeServlet() {
        openProduction();
        Assert.assertEquals("true", getInfoValue("Production mode"));

    }

    @Test
    public void nonProductionModeServlet() {
        open();
        Assert.assertEquals("false", getInfoValue("Production mode"));

    }

    private String getInfoValue(String string) {
        String prefix = string + ": ";
        List<WebElement> divs = findElement(By.className("infoContainer"))
                .findElements(By.tagName("div"));
        Optional<String> infoText = divs.stream().map(WebElement::getText)
                .filter(text -> text.startsWith(prefix)).findFirst();

        return infoText.get().replace(prefix, "");
    }
}
