package com.vaadin.hummingbird.uitest.ui.webcomponent;
/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class ProgressBubbleIT extends PhantomJSTest {

    @Test
    public void domCorrect() {
        open();
        WebElement bubble = findElement(By.tagName("progress-bubble"));
        WebElement content = bubble
                .findElement(By.xpath("./div[@id='content']"));
        Assert.assertEquals("0 %", content.getText());
    }

    @Test
    public void updatesWork() {
        open();
        List<WebElement> bubbles = findElements(By.tagName("progress-bubble"));
        Assert.assertEquals(4, bubbles.size());

        WebElement makeProgress = findElement(By.id("makeProgress"));
        makeProgress.click();
        makeProgress.click();

        for (WebElement bubble : bubbles) {
            Assert.assertEquals(10, getPropertyLong(bubble, "value"));
        }
    }

    private long getPropertyLong(WebElement bubble, String property) {
        return (long) executeScript("return arguments[0][arguments[1]];",
                bubble, property);

    }
}
