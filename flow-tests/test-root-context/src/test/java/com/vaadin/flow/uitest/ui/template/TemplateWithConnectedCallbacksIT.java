/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplateWithConnectedCallbacksIT extends ChromeBrowserTest {

    @Test
    public void checkMessageWrittenFromServer() {
        open();
        assertMessageIsWrittenFromServer();

        WebElement button = findElement(By.id("toggle-button"));

        scrollIntoViewAndClick(button);
        waitForElementNotPresent(
                By.tagName("template-with-connected-callbacks"));
        scrollIntoViewAndClick(button);
        assertMessageIsWrittenFromServer();
    }

    private void assertMessageIsWrittenFromServer() {
        waitForElementPresent(By.tagName("template-with-connected-callbacks"));
        TestBenchElement element = $("template-with-connected-callbacks")
                .first();
        TestBenchElement messageElement = element.$(TestBenchElement.class)
                .id("connectedMessage");

        Assert.assertEquals("Connected (checked from server side)",
                messageElement.getText());
    }

}
