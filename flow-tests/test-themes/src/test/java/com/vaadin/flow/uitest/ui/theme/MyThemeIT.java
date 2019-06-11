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
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class MyThemeIT extends ChromeBrowserTest {

    @Test
    public void loadBaseComponent() {
        getDriver().get(getRootURL()
                + "/view/com.vaadin.flow.uitest.ui.theme.MyComponentView");

        WebElement element = findElement(By.tagName("my-component"));
        Assert.assertFalse(
                findInShadowRoot(element, By.id("component")).isEmpty());
    }

    @Test
    public void loadThemeComponent() {
        getDriver().get(getRootURL()
                + "/view/com.vaadin.flow.uitest.ui.theme.MyThemeComponentView");

        Assert.assertTrue(findElement(By.id("theme-component")).isDisplayed());
    }

}
