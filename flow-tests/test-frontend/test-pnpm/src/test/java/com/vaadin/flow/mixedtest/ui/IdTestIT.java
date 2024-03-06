/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.mixedtest.ui;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Ignore("Fails in Containers")
public class IdTestIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/context-path/servlet-path/route-path";
    }

    @Test
    public void testIds() {
        open();

        checkLogsForErrors(
                msg -> msg.contains("sockjs-node") || msg.contains("[WDS]"));
        waitUntilWithMessage(
                ExpectedConditions
                        .presenceOfElementLocated(By.tagName("my-component")),
                "Failed to load my-component", 25);

        TestBenchElement myComponent = $("my-component").first();

        // wait for polymer initalisation
        waitUntillWithMessage(driver -> getCommandExecutor().executeScript(
                "return !!window.Polymer || !!arguments[0].constructor.polymerElementVersion",
                myComponent),
                "Failed to load constructor.polymerElementVersion for 'my-component'");

        waitUntillWithMessage(
                driver -> getCommandExecutor().executeScript(
                        "return arguments[0].$ !== undefined", myComponent),
                "Failed to load $ for 'my-component'");

        WebElement content = myComponent.$(TestBenchElement.class)
                .id("content");
        Assert.assertEquals("", content.getText());

        WebElement button = myComponent.$(TestBenchElement.class).id("button");
        button.click();
        Assert.assertEquals("1", content.getText());

        button.click();
        Assert.assertEquals("2", content.getText());
    }

    private void waitUntillWithMessage(ExpectedCondition<?> condition,
            String message) {
        waitUntilWithMessage(condition, message, 10);
    }

    private void waitUntilWithMessage(ExpectedCondition<?> condition,
            String message, long time) {
        try {
            waitUntil(condition, time);
        } catch (TimeoutException te) {
            Assert.fail(message);
        }
    }
}
