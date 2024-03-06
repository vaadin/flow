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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PolymerModelPropertiesIT extends ChromeBrowserTest {

    @Test
    public void propertySyncWithModel() {
        open();

        WebElement initial = findElement(By.id("property-value"));
        Assert.assertEquals("Property value:foo, model value: foo",
                initial.getText());

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement input = template.$(TestBenchElement.class).id("input");
        input.clear();
        input.sendKeys("x" + Keys.TAB);

        // property update event comes immediately
        List<WebElement> propertyUpdates = findElements(
                By.id("property-update-event"));
        WebElement propertyUpdate = propertyUpdates
                .get(propertyUpdates.size() - 1);
        Assert.assertEquals("Property value:x, model value: x",
                propertyUpdate.getText());

        // now move focus out of the input and check that value change event is
        // fired
        propertyUpdate.click();

        List<WebElement> valueUpdates = findElements(By.id("value-update"));
        WebElement valueUpdate = valueUpdates.get(valueUpdates.size() - 1);
        Assert.assertEquals("Property value:x, model value: x",
                valueUpdate.getText());
    }
}
