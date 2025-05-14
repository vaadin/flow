/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WebStorageIT extends ChromeBrowserTest {

    @Test
    public void testWebStorageSetAndRemove() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detect"));
        WebElement remove = findElement(By.id("remove"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        remove.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }

    @Test
    public void testWebStorageSetAndClear() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detect"));
        WebElement clear = findElement(By.id("clear"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        clear.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }

    @Test
    public void testWebStorageSetAndRemove_completableFuture() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detectCF"));
        WebElement remove = findElement(By.id("remove"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        remove.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }

    @Test
    public void testWebStorageSetAndClear_completableFuture() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detectCF"));
        WebElement clear = findElement(By.id("clear"));

        input.clear();
        input.sendKeys("foobar", "\n");

        set.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"), "foobar"));

        clear.click();
        detect.click();

        waitUntil(ExpectedConditions.textToBe(By.id("msg"),
                WebStorageView.VALUE_NOT_SET));
    }
}
