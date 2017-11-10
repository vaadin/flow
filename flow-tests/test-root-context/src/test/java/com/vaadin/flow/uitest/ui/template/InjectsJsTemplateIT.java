/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class InjectsJsTemplateIT extends ChromeBrowserTest {

    @Test
    public void executeJsOnInjectedElement() {
        open();

        WebElement parent = findElement(By.tagName("injects-js-template"));

        WebElement injectedTemplate = getInShadowRoot(parent,
                By.id("injected-template"));

        WebElement fooLabel = getInShadowRoot(injectedTemplate,
                By.id("foo-prop"));
        Assert.assertEquals("bar", fooLabel.getText());

        WebElement bazLabel = getInShadowRoot(injectedTemplate,
                By.id("baz-prop"));
        Assert.assertEquals("setFromParent", bazLabel.getText());

        WebElement injectedDiv = getInShadowRoot(parent, By.id("injected-div"));
        Assert.assertEquals("setFromParent", injectedDiv.getAttribute("class"));
    }
}
