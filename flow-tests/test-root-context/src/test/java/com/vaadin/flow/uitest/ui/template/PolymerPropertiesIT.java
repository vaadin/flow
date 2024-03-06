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

public class PolymerPropertiesIT extends ChromeBrowserTest {

    @Test
    public void propertyAdd_propertyBecomesAvailable() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");

        template.$(TestBenchElement.class).id("set-property").click();

        TestBenchElement name = template.$(TestBenchElement.class).id("name");
        Assert.assertEquals("foo", name.getText());
    }
}
