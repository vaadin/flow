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
package com.vaadin.flow.uitest.ui.littemplate;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ReattachIT extends ChromeBrowserTest {

    @Test
    public void reattachedTemplateHasExplicitlySetText() {
        open();

        WebElement button = findElement(By.id("click"));

        // attach template
        button.click();

        TestBenchElement template = $(TestBenchElement.class)
                .id("form-template");
        TestBenchElement div = template.$(TestBenchElement.class).id("div");

        Assert.assertEquals("foo", div.getText());

        // detach template
        button.click();

        // re-attach template
        button.click();

        template = $(TestBenchElement.class).id("form-template");
        div = template.$(TestBenchElement.class).id("div");
        Assert.assertEquals("foo", div.getText());
    }

}
