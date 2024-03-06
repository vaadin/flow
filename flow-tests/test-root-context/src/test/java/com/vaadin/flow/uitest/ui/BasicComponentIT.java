/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class BasicComponentIT extends AbstractBasicElementComponentIT {

    @Test
    public void tagsInText() {
        open();
        WebElement root = findElement(By.id("root"));

        // Selenium does not support text nodes...
        Assert.assertEquals(
                BasicComponentView.TEXT + "\n" + BasicComponentView.DIV_TEXT
                        + "\n" + BasicComponentView.BUTTON_TEXT,
                root.getText());
    }
}
