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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DebounceSynchronizePropertyIT
        extends AbstractDebounceSynchronizeIT {
    private WebElement input;

    @Before
    public void setUp() {
        open();
        input = findElement(By.id("input"));
    }

    @Test
    public void eager() {
        toggleMode("eager");
        assertEager(input);

        toggleMode("eager");
        assertMessages("a", "ab");
    }

    @Test
    public void filtered() {
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
        toggleMode("debounce");
        assertDebounce(input);
    }

    @Test
    @Ignore
    public void throttle() throws InterruptedException {
        toggleMode("throttle");
        assertThrottle(input);
    }

    private void toggleMode(String name) {
        findElement(By.id(name)).click();
    }

}
