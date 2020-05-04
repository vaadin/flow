/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.dependencies;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public abstract class AbstractFrontendInlineIT extends ChromeBrowserTest {

    @Test
    public void inlineDependeciesWithFrontendProtocol() {
        open();

        checkLogsForErrors(msg -> msg.contains("HTML Imports is deprecated"));

        waitUntil(driver -> !driver.findElement(By.className("v-loading-indicator")).isDisplayed());

        WebElement templateElement = $(TestBenchElement.class).id("template")
                .$(DivElement.class).id("frontend-inline");

        Assert.assertEquals("Inline HTML loaded via frontent protocol",
                templateElement.getText());

        String color = templateElement.getCssValue("color");
        Assert.assertEquals("rgba(0, 128, 0, 1)", color);

        WebElement js = findElement(By.id("js"));
        Assert.assertEquals("Inlined JS", js.getText());
    }

}
