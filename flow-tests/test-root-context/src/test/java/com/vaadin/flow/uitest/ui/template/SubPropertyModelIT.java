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
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class SubPropertyModelIT extends ChromeBrowserTest {

    @Test
    public void subproperties() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        Assert.assertEquals("message",
                template.$(TestBenchElement.class).id("msg").getText());

        template.$(TestBenchElement.class).id("button").click();

        Assert.assertEquals("Updated",
                template.$(TestBenchElement.class).id("msg").getText());

        template.$(TestBenchElement.class).id("sync").click();

        WebElement syncedReport = findElement(By.id("synced-msg"));
        Assert.assertEquals("Set from the client", syncedReport.getText());

        TestBenchElement input = template.$(TestBenchElement.class).id("input");
        input.clear();
        input.sendKeys("foo");

        List<WebElement> valueUpdate = findElements(By.id("value-update"));
        Optional<WebElement> result = valueUpdate.stream()
                .filter(element -> element.getText().equals("foo")).findAny();
        Assert.assertTrue("Unable to find updated input value element. "
                + "Looks like input hasn't sent an event for subproperty",
                result.isPresent());

        // click message
        template.$(TestBenchElement.class).id("msg").click();

        Assert.assertEquals(
                "Clicking status message did not get the same modelData as in the message box.",
                template.$(TestBenchElement.class).id("msg").getText(),
                findElement(By.id("statusClick")).getText());
    }
}
