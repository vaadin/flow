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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ConvertToBeanIT extends ChromeBrowserTest {

    @Test
    public void convertToBean_valuesAreUpdated() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        template.$(TestBenchElement.class).id("day").sendKeys("2");
        template.$(TestBenchElement.class).id("month").sendKeys("5");
        template.$(TestBenchElement.class).id("year").sendKeys("2000");

        template.$(TestBenchElement.class).id("click").click();

        String text = template.$(TestBenchElement.class).id("msg").getText();
        Assert.assertEquals("02.05.2000", text);
    }
}
