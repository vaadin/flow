/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.littemplate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class InjectingTemplateIT extends ChromeBrowserTest {

    @Test
    public void mapTemplateViaIdWithNumberProperty_propertyTypeIsNotChangedAfterBidning() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("injecting");
        template.$(TestBenchElement.class).id("show-type").click();

        TestBenchElement container = template.$(TestBenchElement.class)
                .id("container");
        Assert.assertEquals("number", container.getText());
    }
}
