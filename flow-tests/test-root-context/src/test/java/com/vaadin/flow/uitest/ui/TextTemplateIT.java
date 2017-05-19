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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 *
 */
public class TextTemplateIT extends ChromeBrowserTest {

    @Test
    public void checkTextBinding() {
        open();

        // Test plain text binding (no JS expression)
        WebElement textDiv = findElement(By.id("text"));

        Assert.assertEquals("Foo", textDiv.getText());
        Assert.assertEquals("Foo", getLastLabel("plain-text"));

        WebElement button = findElement(By.id("set-simple-name"));
        button.click();

        Assert.assertEquals("Bar", textDiv.getText());
        Assert.assertEquals("Bar", getLastLabel("plain-text"));

        // Test JS expression text binding

        WebElement jsTextDiv = findElement(By.id("js-expression"));

        Assert.assertEquals("No name", jsTextDiv.getText());
        Assert.assertEquals("No name", getLastLabel("js-text"));

        button = findElement(By.id("set-expression-name"));
        button.click();

        Assert.assertEquals("Hello Foo", jsTextDiv.getText());
        Assert.assertEquals("Hello Foo", getLastLabel("js-text"));
    }

    /**
     * Last label contains server side text value.
     */
    private String getLastLabel(String cssStyle) {
        List<WebElement> labels = findElements(By.cssSelector("." + cssStyle));
        return labels.get(labels.size() - 1).getText();
    }
}
