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
package com.vaadin.flow.demo.views;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import org.openqa.selenium.By;
import com.vaadin.ui.textfield.TextField;

/**
 * Integration tests for the {@link TextField}.
 */
public class TextFieldIT extends ComponentDemoTest {
    @Override
    protected String getTestPath() {
        return "/vaadin-text-field";
    }

    @Test
    public void valueChangeListenerReportsCorrectValues() {
        WebElement textFieldValueDiv = layout
                .findElement(By.id("text-field-value"));
        WebElement textField = layout
                .findElement(By.id("text-field-with-value-change-listener"));

        textField.sendKeys("a");
        waitUntilTextsEqual("Text field value changed from '' to 'a'",
                textFieldValueDiv.getText());

        textField.sendKeys(Keys.BACK_SPACE);
        waitUntilTextsEqual("Text field value changed from 'a' to ''",
                textFieldValueDiv.getText());
    }

    @Test
    public void textFieldHasPlaceholder() {
        WebElement textField = layout
                .findElement(By.id("text-field-with-value-change-listener"));
        Assert.assertEquals(textField.getAttribute("placeholder"),
                "placeholder text");
    }

    private void waitUntilTextsEqual(String expected, String actual) {
        waitUntil(driver -> expected.equals(actual));
    }
}
