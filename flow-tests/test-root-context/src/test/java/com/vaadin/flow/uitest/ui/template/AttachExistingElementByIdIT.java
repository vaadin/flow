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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AttachExistingElementByIdIT extends ChromeBrowserTest {

    @Test
    public void elementsAreBoundOnTheServerSide() {
        open();

        WebElement input = getInput();

        Assert.assertEquals("default", getLabel().getText());

        Assert.assertEquals("Type here to update label",
                input.getAttribute("placeholder"));

        input.sendKeys("Harley!");
        input.sendKeys(Keys.ENTER);

        Assert.assertEquals("Text from input Harley!", getLabel().getText());

        // Reset values to defaults
        getInShadowRoot(findElement(By.id("template")), By.id("button")).get().click();

        Assert.assertEquals("default", getLabel().getText());
    }

    private WebElement getInput() {
        return getInShadowRoot(findElement(By.id("template")), By.id("input")).get();
    }

    private WebElement getLabel() {
        return getInShadowRoot(findElement(By.id("template")), By.id("label")).get();
    }
}
