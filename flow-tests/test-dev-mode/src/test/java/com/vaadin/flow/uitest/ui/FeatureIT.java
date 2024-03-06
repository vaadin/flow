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
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.html.testbench.H2Element;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.DevToolsElement;
import com.vaadin.testbench.TestBenchElement;

public class FeatureIT extends ChromeBrowserTest {

    @Test
    public void enableAndDisableFeature() {
        open();
        Lookup lookup = Mockito.mock(Lookup.class);
        String featureProperty = new FeatureFlags(lookup).getFeatures().get(1)
                .getId();
        Assert.assertEquals("Feature file missing",
                $(H2Element.class).id("value").getText());

        // enable the feature
        toggleFirstFeature(false);

        try {
            $(NativeButtonElement.class).id("check").click();
            Assert.assertEquals(
                    "Feature file exists with properties: com.vaadin.experimental."
                            + featureProperty,
                    $(H2Element.class).id("value").getText());

            // disable the feature
            toggleFirstFeature(true);

            $(NativeButtonElement.class).id("check").click();
            Assert.assertEquals("Feature file exists with properties:",
                    $(H2Element.class).id("value").getText());
        } finally {
            $(NativeButtonElement.class).id("remove").click();
            Assert.assertEquals("Feature file missing",
                    $(H2Element.class).id("value").getText());
        }
    }

    private void toggleFirstFeature(boolean expectedInitialState) {
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();
        devTools.expand();
        devTools.$(NativeButtonElement.class).id("features").click();
        TestBenchElement toggleButton = devTools.$(TestBenchElement.class)
                .attributeContains("class", "feature-toggle").get(0);
        String checked = executeScript("return arguments[0].checked",
                toggleButton).toString();
        Assert.assertEquals(
                "Toggle button state expected " + expectedInitialState,
                Boolean.toString(expectedInitialState), checked);
        toggleButton.click();
    }
}
