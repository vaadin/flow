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
package com.vaadin.flow.test.components;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

@TestFor(CompositeView.class)
public class CompositeIT extends AbstractDefaultIT {

    @BrowserTest
    public void changeOnClient() {
        open();
        WebElement name = findElement(By.id(CompositeNestedView.NAME_ID));
        InputTextElement input = $(InputTextElement.class)
                .id(CompositeNestedView.NAME_FIELD_ID);
        Assertions.assertEquals("Name on server:", name.getText());
        input.setValue("123");
        Assertions.assertEquals("Name on server: 123", name.getText());

        InputTextElement serverValueInput = $(InputTextElement.class)
                .id(CompositeView.SERVER_INPUT_ID);
        WebElement serverValueButton = findElement(
                By.id(CompositeView.SERVER_INPUT_BUTTON_ID));

        serverValueInput.setValue("server value");
        serverValueButton.click();

        Assertions.assertEquals("Name on server: server value", name.getText());

    }

    @BrowserTest
    public void htmlImportOfContentLoaded() {
        open();
        waitForElementPresent(By.id(CompositeView.COMPOSITE_SLIDER));
        TestBenchElement slider = (TestBenchElement) findElement(
                By.id(CompositeView.COMPOSITE_SLIDER));
        Assertions.assertEquals(Integer.valueOf(100),
                slider.getPropertyInteger("max"));

    }
}
