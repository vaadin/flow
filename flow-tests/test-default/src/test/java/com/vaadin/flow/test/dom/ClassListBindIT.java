/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.dom;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(ClassListBindView.class)
public class ClassListBindIT extends AbstractDefaultIT {

    @BrowserTest
    public void toggleSignal_updatesClassAndStyling() {
        open();

        DivElement target = $(DivElement.class).id("target");
        NativeButtonElement toggle = $(NativeButtonElement.class).id("toggle");

        // Initially: highlight is false -> default color
        String initialColor = target.getCssValue("color");
        assertNotEquals("rgba(255, 0, 0, 1)", initialColor);
        String classAttribute = target.getAttribute("class");
        assertNotNull(classAttribute);
        assertFalse(classAttribute.contains("highlight"));

        // Toggle on
        toggle.click();
        waitUntil(
                d -> "rgba(255, 0, 0, 1)".equals(target.getCssValue("color")));
        classAttribute = target.getAttribute("class");
        assertNotNull(classAttribute);
        assertTrue(classAttribute.contains("highlight"));

        // Toggle off
        toggle.click();
        waitUntil(
                d -> !"rgba(255, 0, 0, 1)".equals(target.getCssValue("color")));
        classAttribute = target.getAttribute("class");
        assertNotNull(classAttribute);
        assertFalse(classAttribute.contains("highlight"));
    }
}
