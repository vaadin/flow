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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class AttachExistingDomElementByIdIT extends ChromeBrowserTest {

    @Test
    public void elementsAreBoundOnTheServerSide() {
        open();

        assertTemplate("template", "bar", "Foo");
    }

    protected void assertTemplate(String id, String initialLabelText,
            String placeholder) {
        WebElement input = getInput(id);

        Assert.assertEquals(initialLabelText, getLabel(id).getText());

        Assert.assertEquals(placeholder, input.getAttribute("placeholder"));

        input.sendKeys("Harley!");
        input.sendKeys(Keys.TAB);

        Assert.assertEquals("Text from input Harley!", getLabel(id).getText());

        // Reset values to defaults
        $(TestBenchElement.class).id(id).$(TestBenchElement.class).id("button")
                .click();

        Assert.assertEquals("default", getLabel(id).getText());
    }

    private WebElement getInput(String id) {
        return $(TestBenchElement.class).id(id).$(TestBenchElement.class)
                .id("input");
    }

    private WebElement getLabel(String id) {
        return $(TestBenchElement.class).id(id).$(TestBenchElement.class)
                .id("label");
    }
}
