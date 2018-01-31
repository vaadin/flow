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

public class HiddenTemplateIT extends ChromeBrowserTest {

    @Test
    public void initiallyHiddenElementStaysHidden() {
        open();

        WebElement template = findElement(By.id("template"));
        WebElement child = getInShadowRoot(template, By.id("child"));
        Assert.assertNotNull(child.getAttribute("hidden"));

        WebElement visibility = getInShadowRoot(template, By.id("visibility"));
        visibility.click();
        Assert.assertNotNull(child.getAttribute("hidden"));

        visibility.click();
        Assert.assertNotNull(child.getAttribute("hidden"));
    }

    @Test
    public void initiallyNotHiddenElementChangesItsVisibility() {
        open();

        WebElement template = findElement(By.id("template"));

        WebElement hidden = getInShadowRoot(template, By.id("hidden"));
        hidden.click();

        WebElement child = getInShadowRoot(template, By.id("child"));
        Assert.assertNull(child.getAttribute("hidden"));

        WebElement visibility = getInShadowRoot(template, By.id("visibility"));
        visibility.click();
        Assert.assertNotNull(child.getAttribute("hidden"));

        visibility.click();
        Assert.assertNull(child.getAttribute("hidden"));
    }
}
