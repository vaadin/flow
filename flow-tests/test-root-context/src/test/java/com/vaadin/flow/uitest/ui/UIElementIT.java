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
package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.By;

public class UIElementIT extends ChromeBrowserTest {

    @Test
    public void uiElementWorksInJSCalls() {
        open();

        List<WebElement> bodyChildrenAddedViaJs = findElements(
                By.className("body-child"));
        Assert.assertEquals(1, bodyChildrenAddedViaJs.size());

        findElement(By.tagName("button")).click();

        bodyChildrenAddedViaJs = findElements(By.className("body-child"));
        Assert.assertEquals(2, bodyChildrenAddedViaJs.size());
    }
}
