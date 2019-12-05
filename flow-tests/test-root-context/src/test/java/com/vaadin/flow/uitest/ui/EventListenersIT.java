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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

public class EventListenersIT extends ChromeBrowserTest {

    @Test
    public void clickListenerIsCalledOnlyOnce() {
        open();

        WebElement button = findElement(By.id("click"));
        button.click();

        List<WebElement> clicks = findElements(By.className("count"));
        Assert.assertEquals(1, clicks.size());

        Assert.assertEquals("1", clicks.get(0).getText());

        button.click();

        clicks = findElements(By.className("count"));

        Assert.assertEquals(2, clicks.size());
        Assert.assertEquals("1", clicks.get(0).getText());
        Assert.assertEquals("2", clicks.get(1).getText());
    }
}
