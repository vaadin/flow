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
package com.vaadin.flow.uitest.ui;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DebounceSynchronizePropertyIT extends ChromeBrowserTest {
    @Test
    public void eager() {
        open();
        WebElement input = findElement(By.id("input"));

        toggleMode("eager");

        input.sendKeys("a");
        assertMessages("a");

        input.sendKeys("b");
        assertMessages("a", "ab");

        toggleMode("eager");
        assertMessages("a", "ab");
    }

    @Test
    public void filtered() {
        open();
        WebElement input = findElement(By.id("input"));

        toggleMode("filtered");

        input.sendKeys("a");
        assertMessages();

        input.sendKeys("b");
        assertMessages("ab");

        input.sendKeys("c");
        assertMessages("ab");

        input.sendKeys("d");
        assertMessages("ab", "abcd");
    }

    @Test
    public void debounce() throws InterruptedException {
        open();
        WebElement input = findElement(By.id("input"));

        toggleMode("debounce");

        // Should not sync while typing within 1000ms from last time
        for (String keys : Arrays.asList("a", "b", "c")) {
            input.sendKeys(keys);
            Thread.sleep(500);
            assertMessages();
        }

        // Should sync after some additional inactivity
        Thread.sleep(700);
        assertMessages("abc");
    }

    @Test
    public void lazy() throws InterruptedException {
        open();
        WebElement input = findElement(By.id("input"));

        toggleMode("lazy");

        input.sendKeys("a");
        assertMessages("a");

        Thread.sleep(700);
        input.sendKeys("b");

        // T + 700, only first update registered
        assertMessages("a");

        Thread.sleep(800);

        // T + 1500, second update registered
        assertMessages("a", "ab");
        input.sendKeys("c");
        assertMessages("a", "ab");

        Thread.sleep(700);

        // T + 2200, third update registered
        assertMessages("a", "ab", "abc");
    }

    private void assertMessages(String... expectedMessages) {
        Assert.assertArrayEquals(expectedMessages,
                findElements(By.cssSelector("#messages p")).stream()
                        .map(WebElement::getText)
                        .map(text -> text.replaceFirst("Value: ", ""))
                        .toArray(String[]::new));
    }

    private void toggleMode(String name) {
        findElement(By.id(name)).click();
    }

}
