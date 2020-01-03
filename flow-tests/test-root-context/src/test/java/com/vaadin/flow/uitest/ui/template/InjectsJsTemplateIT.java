/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class InjectsJsTemplateIT extends ChromeBrowserTest {

    @Test
    public void executeJsOnInjectedElement() {
        open();

        TestBenchElement parent = $("injects-js-template").first();

        TestBenchElement injectedTemplate = parent.$(TestBenchElement.class)
                .id("injected-template");

        WebElement fooLabel = injectedTemplate.$(TestBenchElement.class)
                .id("foo-prop");
        Assert.assertEquals("bar", fooLabel.getText());

        WebElement bazLabel = injectedTemplate.$(TestBenchElement.class)
                .id("baz-prop");
        Assert.assertEquals("setFromParent", bazLabel.getText());

        WebElement injectedDiv = parent.$(TestBenchElement.class)
                .id("injected-div");
        Assert.assertEquals("setFromParent", injectedDiv.getAttribute("class"));
    }
}
