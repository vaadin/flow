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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WebComponentIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/index.html";
    }

    @Test
    public void indexPageGetsWebComponent_attributeIsReflectedToServer() {
        open();

        waitForElementVisible(By.id("show-message"));

        WebElement showMessage = findElement(By.id("show-message"));
        WebElement select = showMessage.findElement(By.cssSelector("select"));

        // Selection is visibly changed and event manually dispatched
        // as else the change is not seen.
        getCommandExecutor().executeScript("arguments[0].value='Peter';"
                + "arguments[0].dispatchEvent(new Event('change'));", select);

        Assert.assertEquals("Selected: Peter, Parker",
                showMessage.findElement(By.cssSelector("span")).getText());

        WebElement noMessage = findElement(By.id("no-message"));

        select = noMessage.findElement(By.cssSelector("select"));
        getCommandExecutor().executeScript("arguments[0].value='Peter';"
                + "arguments[0].dispatchEvent(new Event('change'));", select);

        Assert.assertFalse("Message should not be visible",
                noMessage.findElement(By.cssSelector("span")).isDisplayed());
    }
}
