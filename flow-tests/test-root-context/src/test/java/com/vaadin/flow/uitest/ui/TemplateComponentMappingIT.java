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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TemplateComponentMappingIT extends ChromeBrowserTest {

    @Test
    public void eventHandler() {
        open();
        WebElement button = findElement(
                By.id(TemplateComponentMappingView.BUTTON_ID));
        button.click();
        Assert.assertEquals("client: button was clicked\n" //
                + "" + "server: button was clicked", getLog());
        WebElement span = findElement(
                By.id(TemplateComponentMappingView.SPAN_ID));
        span.click();
        Assert.assertEquals("client: button was clicked\n" //
                + "" + "server: button was clicked\n" //
                + "client: span was clicked\n" //
                + "server: span was clicked", getLog());
    }

    @Test
    public void synchronizedProperty() {
        open();
        WebElement input = findElement(
                By.id(TemplateComponentMappingView.INPUT_ID));
        input.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, Keys.BACK_SPACE,
                "Hello" + Keys.TAB);
        blurInput();
        Assert.assertEquals("client: input was changed to Hello\n" //
                + "server: input value changed to Hello", getLog());
    }

    private void blurInput() {
        findElement(By.xpath("//body")).click();
    }

    private String getLog() {
        return findElement(By.id(TemplateComponentMappingView.LOG_ID))
                .getText();
    }
}
