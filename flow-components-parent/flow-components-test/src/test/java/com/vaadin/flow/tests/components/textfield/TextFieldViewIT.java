/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tests.components.textfield;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;
import com.vaadin.testbench.By;
import com.vaadin.ui.TextField;

/**
 * Integration tests for {@link TextField}.
 */
public class TextFieldViewIT extends AbstractComponentIT {

    @Before
    public void init() {
        open();
    }

    @Test
    public void assertRequired() {
        WebElement webComponent = findElement(By.tagName("vaadin-text-field"));

        WebElement textField = getInShadowRoot(webComponent, By.id("input"));
        Assert.assertNull(textField.getAttribute("required"));

        WebElement button = findElement(By.id("required"));
        button.click();
        waitUntil(driver -> textField.getAttribute("required"));

        button.click();
        waitUntil(driver -> textField.getAttribute("required") == null);
    }
}
