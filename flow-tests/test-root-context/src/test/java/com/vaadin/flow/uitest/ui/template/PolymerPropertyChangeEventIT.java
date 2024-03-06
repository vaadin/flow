/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PolymerPropertyChangeEventIT extends ChromeBrowserTest {

    @Test
    public void propertyChangeEvent() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        template.$(TestBenchElement.class).id("input").sendKeys("foo");

        List<WebElement> changeEvents = findElements(
                By.className("change-event"));
        Assert.assertTrue("Expected property change event is not fired. "
                + "Element with expected old and new value is not found",
                changeEvents.stream().anyMatch(
                        event -> "New property value: 'foo', old property value: 'fo'"
                                .equals(event.getText())));
    }
}
