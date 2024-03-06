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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class BasicTypeInListIT extends ChromeBrowserTest {

    @Test
    public void basicTypeInModeList() {
        open();
        TestBenchElement template = $(TestBenchElement.class).id("template");
        List<TestBenchElement> items = template.$(TestBenchElement.class)
                .attribute("class", "item").all();

        Assert.assertEquals(2, items.size());
        Assert.assertEquals("foo", items.get(0).getText());
        Assert.assertEquals("bar", items.get(1).getText());

        findElement(By.id("add")).click();

        items = template.$(TestBenchElement.class).attribute("class", "item")
                .all();

        Assert.assertEquals(3, items.size());
        Assert.assertEquals("newItem", items.get(2).getText());

        findElement(By.id("remove")).click();

        items = template.$(TestBenchElement.class).attribute("class", "item")
                .all();

        Assert.assertEquals(2, items.size());
        Assert.assertEquals("bar", items.get(0).getText());
    }
}
