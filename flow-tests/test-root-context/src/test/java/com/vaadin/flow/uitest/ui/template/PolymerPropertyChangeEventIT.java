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
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PolymerPropertyChangeEventIT extends ChromeBrowserTest {

    @Test
    public void propertyChangeEvent() {
        open();

        TestBenchElement template = $(TestBenchElement.class).id("template");
        template.$(TestBenchElement.class).id("input").sendKeys("foo");

        List<WebElement> changeEvents = findElements(
                By.className("change-event"));
        Assert.assertTrue("Expected property change event is not fired. "
                + "Element with expected old and new value is not found",
                changeEvents.stream().anyMatch(
                        event -> "New property value: 'foo', old property value: 'fo'"
                                .equals(event.getText())));
    }
}
