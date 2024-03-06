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

public class MutationSeveralSyncedPropsIT extends ChromeBrowserTest {

    @Test
    public void twoSynchronizedPropertiesSimultensousUpdate_bothAreUpdated() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");

        WebElement name = template.$(TestBenchElement.class).id("name");
        WebElement msg = template.$(TestBenchElement.class).id("msg");

        Assert.assertEquals("foo", name.getText());
        Assert.assertEquals("msg", msg.getText());

        findElement(By.id("update")).click();

        Assert.assertEquals("bar", name.getText());
        Assert.assertEquals("baz", msg.getText());
    }
}
