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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class BasicElementIT extends AbstractBasicElementComponentIT {

    // #671, #1231
    @Test
    public void testAddRemoveComponentDuringSameRequest() {
        open();
        findElement(By.id("addremovebutton")).click();

        List<WebElement> addremovecontainerChildren = findElement(
                By.id("addremovecontainer")).findElements(By.tagName("div"));
        Assert.assertEquals(2, addremovecontainerChildren.size());
        Assert.assertEquals("to-remove",
                addremovecontainerChildren.get(0).getAttribute("id"));
        Assert.assertEquals("ok",
                addremovecontainerChildren.get(1).getAttribute("id"));
        // verify the UI still works
        assertDomUpdatesAndEventsDoSomething();
    }
}
