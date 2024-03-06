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

public class PolymerPropertyMutationInObserverIT extends ChromeBrowserTest {

    @Test
    public void property_mutation_inside_observers_synced_correctly() {
        open();

        List<WebElement> modelValueDivs = findElements(
                By.className("model-value"));
        Assert.assertEquals("Value changed twice initially", 2,
                modelValueDivs.size());
        Assert.assertEquals(
                "First value change should equal the initially set server side value",
                "Event old value: null, event value: initially set value, current model value: initially set value",
                modelValueDivs.get(0).getText());
        Assert.assertEquals(
                "Observer mutation has been transmitted to the server",
                "Event old value: initially set value, event value: mutated, current model value: mutated",
                modelValueDivs.get(1).getText());

        TestBenchElement template = $(TestBenchElement.class).id("template");
        template.$(TestBenchElement.class).id("input")
                .sendKeys(Keys.BACK_SPACE);

        modelValueDivs = findElements(By.className("model-value"));
        Assert.assertEquals("Value changed 4 times in total after backspace", 4,
                modelValueDivs.size());
        Assert.assertEquals("User action mutation synced to server",
                "Event old value: mutated, event value: mutate, current model value: mutated",
                modelValueDivs.get(2).getText());
        Assert.assertEquals(
                "Observer mutation acting on the user action mutation synced to server",
                "Event old value: mutate, event value: mutated, current model value: mutated",
                modelValueDivs.get(3).getText());
    }
}
