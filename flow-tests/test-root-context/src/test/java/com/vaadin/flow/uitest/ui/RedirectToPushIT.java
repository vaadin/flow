/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreOSGi.class)
public class RedirectToPushIT extends ChromeBrowserTest {

    @Test
    public void pushIsSetAfterNavigation() {
        open();

        $(NativeButtonElement.class).first().click();

        // when running V15-Bootstrapping push must be configured in AppShell
        // otherwise @Push annotation in routes are logged in startup but
        // ignored.
        String pushMode = Boolean.getBoolean(
                "vaadin.useDeprecatedV14Bootstrapping") ? "AUTOMATIC"
                        : "DISABLED";

        Assert.assertEquals("Push mode: " + pushMode,
                $(TestBenchElement.class).id("pushMode").getText());
    }
}
