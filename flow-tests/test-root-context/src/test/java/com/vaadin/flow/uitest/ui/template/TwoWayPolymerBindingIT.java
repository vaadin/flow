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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

public class TwoWayPolymerBindingIT extends ChromeBrowserTest {

    @Test
    public void initialModelValueIsPresentAndModelUpdatesNormally() {
        open();

        WebElement template = findElement(By.id("template"));
        WebElement input = getInShadowRoot(template, By.id("input"));

        input.clear();
        input.sendKeys("a");
        Assert.assertEquals("Value: a", getStatusMessage());

        input.sendKeys("b");
        Assert.assertEquals("Value: ab", getStatusMessage());

        getInShadowRoot(template, By.id("reset")).click();
        Assert.assertEquals("Value:", getStatusMessage());
        Assert.assertEquals("", getValueProperty(input));

        input.sendKeys("c");
        Assert.assertEquals("Value: c", getStatusMessage());
    }

    private Object getValueProperty(WebElement input) {
        return ((JavascriptExecutor) getDriver())
                .executeScript("return arguments[0].value", input);
    }

    private String getStatusMessage() {
        WebElement template = findElement(By.id("template"));

        return getInShadowRoot(template, By.id("status")).getText();
    }
}
