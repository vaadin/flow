/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ExecJavaScriptIT extends ChromeBrowserTest {
    @Test
    public void testExecuteJavaScript() {
        open();

        String alertText = getButton("alertButton").getText();
        String focusText = getButton("focusButton").getText();

        getButton("swapButton").click();

        Assert.assertEquals(focusText, getButton("alertButton").getText());
        Assert.assertEquals(alertText, getButton("focusButton").getText());

        getButton("createButton").click();

        WebElement findElement = findElement(By.className("newInput"));
        Assert.assertEquals("Value from js", findElement.getAttribute("value"));
    }

    private WebElement getButton(String id) {
        return findElement(By.id(id));
    }
}
