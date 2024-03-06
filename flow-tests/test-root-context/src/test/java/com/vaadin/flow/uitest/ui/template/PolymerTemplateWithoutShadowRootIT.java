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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PolymerTemplateWithoutShadowRootIT extends ChromeBrowserTest {

    @Test
    public void componentMappedCorrectly() {
        open();
        DivElement content = $(DivElement.class).attribute("real", "deal")
                .first();
        Assert.assertEquals("Hello", content.getText());
        DivElement special = $(DivElement.class).id("special!#id");
        Assert.assertEquals("Special", special.getText());
        DivElement map = $(DivElement.class).id("map");
        Assert.assertEquals("Map", map.getText());
        content.click();
        Assert.assertEquals("Goodbye", content.getText());
    }
}
