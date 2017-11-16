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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class ShadowRootIT extends ChromeBrowserTest {

    @Test
    public void checkShadowRoot() {
        open();

        WebElement div = findElement(By.id("test-element"));
        WebElement shadowDiv = getInShadowRoot(div,
                By.id("shadow-div"));
        Assert.assertEquals("Div inside shadow DOM", shadowDiv.getText());

        WebElement shadowLabel = getInShadowRoot(div,
                By.id("shadow-label"));
        Assert.assertEquals("Label inside shadow DOM",
                shadowLabel.getText());

        findElement(By.id("remove")).click();

        Assert.assertTrue("Child has not been removed from the shadow root",
                findElements(By.id("shadow-label")).isEmpty());
    }
}
