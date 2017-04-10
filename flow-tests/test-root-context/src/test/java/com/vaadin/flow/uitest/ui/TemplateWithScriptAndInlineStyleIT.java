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
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.PhantomJSTest;

public class TemplateWithScriptAndInlineStyleIT extends PhantomJSTest {

    @Test
    public void assertTextAddedByScript() {
        open();
        WebElement foo = findElement(By.id("foo"));
        Assert.assertEquals(
                "bar - set by script, color green from inline style",
                foo.getText());
        Assert.assertEquals("rgba(0, 255, 0, 1)", foo.getCssValue("color"));

        WebElement addedFromScript = findElement(
                By.id("added-from-src-script"));
        Assert.assertEquals("Hello from src script", addedFromScript.getText());
    }
}
