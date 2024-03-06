/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ElementInitOrderIT extends ChromeBrowserTest {

    @Test
    public void elementInitOrder() throws URISyntaxException {
        open();

        assertInitOrder();

        findElement(By.id("reattach")).click();

        assertInitOrder();
    }

    private void assertInitOrder() {
        for (String name : Arrays.asList("init-order-polymer",
                "init-order-nopolymer")) {
            TestBenchElement element = $(name).first();
            String status = element.$(TestBenchElement.class).id("status")
                    .getText();
            Assert.assertEquals(
                    "property = property, attribute = attribute, child count = 1, style = style, class = class",
                    status);
        }
    }
}
