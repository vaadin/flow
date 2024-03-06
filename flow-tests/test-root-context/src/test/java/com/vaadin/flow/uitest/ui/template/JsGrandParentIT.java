/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class JsGrandParentIT extends ChromeBrowserTest {

    @Test
    public void callJsInsideGrandInjected() {
        open();

        TestBenchElement parent = $("js-grand-parent").first();
        TestBenchElement child = parent.$("js-sub-template").first();
        TestBenchElement grandChild = child.$("js-injected-grand-child")
                .first();
        WebElement label = grandChild.$(TestBenchElement.class).id("foo-prop");

        waitUntil(driver -> "bar".equals(label.getText()));
    }
}
