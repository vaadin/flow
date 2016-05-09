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
package com.vaadin.hummingbird.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

/**
 * @author Vaadin Ltd
 *
 */
public class ForTemplateIT extends PhantomJSTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void initial() {
        List<WebElement> lis = findElements(By.cssSelector("li"));
        Assert.assertEquals(2, lis.size());
        checkLi(lis.get(0), "item1", "text1");
        checkLi(lis.get(1), "item2", "text2");
    }

    @Test
    public void append() {
        WebElement append = findElement(By.id("append"));
        append.click();

        List<WebElement> lis = findElements(By.cssSelector("li"));
        Assert.assertEquals(3, lis.size());
        checkLi(lis.get(0), "item1", "text1");
        checkLi(lis.get(1), "item2", "text2");
        checkLi(lis.get(2), "appended", "append");
    }

    @Test
    public void updateFirst() {
        WebElement update = findElement(By.id("update-first"));
        update.click();

        List<WebElement> lis = findElements(By.cssSelector("li"));
        Assert.assertEquals(2, lis.size());
        checkLi(lis.get(0), "Updated first", "update first");
        checkLi(lis.get(1), "item2", "text2");
    }

    @Test
    public void updateSecond() {
        WebElement update = findElement(By.id("update-second"));
        update.click();

        List<WebElement> lis = findElements(By.cssSelector("li"));
        Assert.assertEquals(2, lis.size());
        checkLi(lis.get(0), "item1", "text1");
        checkLi(lis.get(1), "Updated second", "update second");
    }

    @Test
    public void updateLast() {
        WebElement append = findElement(By.id("append"));
        append.click();

        WebElement update = findElement(By.id("update-last"));
        update.click();

        List<WebElement> lis = findElements(By.cssSelector("li"));
        Assert.assertEquals(3, lis.size());
        checkLi(lis.get(0), "item1", "text1");
        checkLi(lis.get(1), "item2", "text2");
        checkLi(lis.get(2), "Updated last", "update last");
    }

    @Test
    public void deleteFirst() {
        WebElement update = findElement(By.id("delete-first"));
        update.click();

        List<WebElement> lis = findElements(By.cssSelector("li"));
        Assert.assertEquals(1, lis.size());
        checkLi(lis.get(0), "item2", "text2");
    }

    private void checkLi(WebElement element, String text, String value) {
        Assert.assertEquals(text, element.getText());
        Assert.assertEquals(value, element.findElement(By.cssSelector("input"))
                .getAttribute("value"));
    }
}
