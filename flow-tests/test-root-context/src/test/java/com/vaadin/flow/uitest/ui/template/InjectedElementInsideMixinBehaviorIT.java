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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class InjectedElementInsideMixinBehaviorIT extends ChromeBrowserTest {

    @Test
    public void injectedByIdWorksWitinMixins() {
        open();

        TestBenchElement template = $("mixin-injects").first();
        WebElement injected = template.$(TestBenchElement.class).id("injected");
        Assert.assertEquals("foo", injected.getText());
    }
}
