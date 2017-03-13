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
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testcategory.ChromeTests;
import com.vaadin.hummingbird.testutil.SingleBrowserTest;
import com.vaadin.testbench.By;

@Category(ChromeTests.class)
public class EventHandlerIT extends SingleBrowserTest {

    @Test
    public void handleEventOnServer() {
        open();

        WebElement template = findElement(By.id("template"));
        getInShadowRoot(template, By.id("handle")).get().click();
        Assert.assertTrue(
                "Unable to find server event handler invocation confirmation. "
                        + "Looks like 'click' event handler has not been invoked on the server side",
                isElementPresent(By.id("event-handler-result")));

        getInShadowRoot(template, By.id("send")).get().click();
        WebElement container = findElement(By.id("event-data"));
        List<WebElement> divs = container.findElements(By.tagName("div"));

        Assert.assertEquals(
                "Unexpected 'button' event data in the received event handler parameter",
                "button: 0", divs.get(1).getText());
        Assert.assertEquals(
                "Unexpected 'type' event data in the received event handler parameter",
                "type: click", divs.get(2).getText());
        Assert.assertEquals(
                "Unexpected 'tag' event data in the received event handler parameter",
                "tag: button", divs.get(3).getText());
    }
}
