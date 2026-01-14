/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClassListBindIT extends ChromeBrowserTest {

    @Test
    public void toggleSignal_updatesClassAndStyling() {
        open("com.vaadin.flow.uitest.ui.ClassListBindView");

        DivElement target = $(DivElement.class).id("target");
        NativeButtonElement toggle = $(NativeButtonElement.class).id("toggle");

        // Initially: highlight is false -> default color
        String initialColor = target.getCssValue("color");
        Assert.assertNotEquals("rgba(255, 0, 0, 1)", initialColor);
        String classAttribute = target.getAttribute("class");
        Assert.assertNotNull(classAttribute);
        Assert.assertFalse(classAttribute.contains("highlight"));

        // Toggle on
        toggle.click();
        waitUntil(
                d -> "rgba(255, 0, 0, 1)".equals(target.getCssValue("color")));
        classAttribute = target.getAttribute("class");
        Assert.assertNotNull(classAttribute);
        Assert.assertTrue(classAttribute.contains("highlight"));

        // Toggle off
        toggle.click();
        waitUntil(
                d -> !"rgba(255, 0, 0, 1)".equals(target.getCssValue("color")));
        classAttribute = target.getAttribute("class");
        Assert.assertNotNull(classAttribute);
        Assert.assertFalse(classAttribute.contains("highlight"));
    }
}
