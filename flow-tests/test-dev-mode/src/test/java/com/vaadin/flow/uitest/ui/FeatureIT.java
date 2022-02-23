/*
 * Copyright 2000-2022 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.H2Element;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.DevModeGizmoElement;
import com.vaadin.testbench.TestBenchElement;

public class FeatureIT extends ChromeBrowserTest {

    @Test
    public void enableAndDisableFeature() {
        open();
        Assert.assertEquals("Feature file missing",
                $(H2Element.class).id("value").getText());

        // enable the feature
        toggleViteFeature(false);

        try {
            $(NativeButtonElement.class).id("check").click();
            Assert.assertEquals(
                    "Feature file exists with properties: com.vaadin.experimental.viteForFrontendBuild",
                    $(H2Element.class).id("value").getText());

            // disable the feature
            toggleViteFeature(true);

            $(NativeButtonElement.class).id("check").click();
            Assert.assertEquals("Feature file exists with properties:",
                    $(H2Element.class).id("value").getText());
        } finally {
            $(NativeButtonElement.class).id("remove").click();
            Assert.assertEquals("Feature file missing",
                    $(H2Element.class).id("value").getText());
        }
    }

    private void toggleViteFeature(boolean expectedInitialState) {
        DevModeGizmoElement gizmo = $(DevModeGizmoElement.class).waitForFirst();
        gizmo.expand();
        gizmo.$(NativeButtonElement.class).id("features").click();
        TestBenchElement toggleButton = gizmo.$(TestBenchElement.class)
                .id("feature-toggle-viteForFrontendBuild");
        String checked = getCommandExecutor().executeScript(
                "return arguments[0].checked", toggleButton).toString();
        Assert.assertEquals(
                "Toggle button state expected " + expectedInitialState,
                Boolean.toString(expectedInitialState), checked);
        toggleButton.click();
    }
}
