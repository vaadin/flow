/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.dom;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(BasicElementView.class)
public class BasicElementIT extends AbstractBasicElementComponentIT {

    // #671, #1231
    @BrowserTest
    public void testAddRemoveComponentDuringSameRequest() {
        open();
        findElement(By.id("addremovebutton")).click();

        List<WebElement> addremovecontainerChildren = findElement(
                By.id("addremovecontainer")).findElements(By.tagName("div"));
        Assertions.assertEquals(2, addremovecontainerChildren.size());
        Assertions.assertEquals("to-remove",
                addremovecontainerChildren.get(0).getAttribute("id"));
        Assertions.assertEquals("ok",
                addremovecontainerChildren.get(1).getAttribute("id"));
        // verify the UI still works
        assertDomUpdatesAndEventsDoSomething();
    }
}
