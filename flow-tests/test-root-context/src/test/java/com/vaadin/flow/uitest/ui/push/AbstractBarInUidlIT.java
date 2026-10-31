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
package com.vaadin.flow.uitest.ui.push;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public abstract class AbstractBarInUidlIT extends AbstractLogTest {

    @Test
    public void barInResponse_messagesAreHandled() {
        open();

        findElement(By.id("click")).click();
        findElement(By.id("click")).click();

        waitUntil(driver -> getMessages().size() == 2);
        Assert.assertEquals(
                List.of(BarInUidlView.CLICKED_TEXT, BarInUidlView.CLICKED_TEXT),
                getMessages());
    }

    @Test
    public void barInPush_messagesAreHandled() {
        open();
        testBench().disableWaitForVaadin();

        findElement(By.id("push")).click();
        findElement(By.id("push")).click();

        waitUntil(driver -> getMessages().size() == 2);
        Assert.assertEquals(
                List.of(BarInUidlView.PUSHED_TEXT, BarInUidlView.PUSHED_TEXT),
                getMessages());
    }

    private List<String> getMessages() {
        return findElements(By.className("message")).stream()
                .map(WebElement::getText).toList();
    }
}
