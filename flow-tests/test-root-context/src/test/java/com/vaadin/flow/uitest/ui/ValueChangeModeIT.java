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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.data.value.ValueChangeMode;

public class ValueChangeModeIT extends AbstractDebounceSynchronizeIT {

    private WebElement input;

    @Before
    public void setUp() {
        open();
        input = findElement(By.id("input"));
    }

    @Test
    public void eager() {
        toggleMode(ValueChangeMode.EAGER);
        assertEager(input);
    }

    @Test
    public void lazy() throws InterruptedException {
        toggleMode(ValueChangeMode.LAZY);
        assertDebounce(input);
    }

    @Test
    @Ignore
    public void timeout() throws InterruptedException {
        toggleMode(ValueChangeMode.TIMEOUT);
        assertThrottle(input);
    }

    @Test
    public void onChange() {
        toggleMode(ValueChangeMode.ON_CHANGE);
        input.sendKeys("a");
        assertMessages();

        input.sendKeys("\n");
        assertMessages("a");
    }

    @Test
    public void onBlur() {
        toggleMode(ValueChangeMode.ON_BLUR);
        input.sendKeys("a");
        assertMessages();

        input.sendKeys("\n");
        assertMessages();

        blur();
        assertMessages("a");
    }

    private void toggleMode(ValueChangeMode mode) {
        WebElement modeButton = findElement(By.id(mode.name()));
        modeButton.click();
    }

}
