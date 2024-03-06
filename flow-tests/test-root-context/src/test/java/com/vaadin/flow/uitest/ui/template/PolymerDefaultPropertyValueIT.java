/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class PolymerDefaultPropertyValueIT extends ChromeBrowserTest {

    @Test
    public void initialModelValues_polymerHasDefaultValues() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        TestBenchElement text = template.$(TestBenchElement.class).id("text");

        Assert.assertEquals("foo", text.getText());

        TestBenchElement name = template.$(TestBenchElement.class).id("name");
        Assert.assertEquals("bar", name.getText());

        TestBenchElement msg = template.$(TestBenchElement.class).id("message");
        Assert.assertEquals("updated-message", msg.getText());

        TestBenchElement email = template.$(TestBenchElement.class).id("email");
        Assert.assertEquals("foo@example.com", email.getText());

        findElement(By.id("show-email")).click();

        WebElement serverSideEmailValue = findElement(By.id("email-value"));
        Assert.assertEquals("foo@example.com", serverSideEmailValue.getText());
    }
}
