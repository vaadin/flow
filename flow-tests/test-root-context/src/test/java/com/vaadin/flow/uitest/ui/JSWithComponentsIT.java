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

public class JSWithComponentsIT extends PhantomJSTest {

    @Test
    public void element$serverWorksOutsideTemplate() {
        open();

        WebElement div = findElement(By.id("div"));
        Assert.assertEquals("initial", div.getText());

        WebElement button1 = findElement(By.id("button1"));
        button1.click();
        Assert.assertEquals("initial-method1", div.getText());

        WebElement button2 = findElement(By.id("button2"));
        button2.click();
        Assert.assertEquals("initial-method1-method2[12]", div.getText());
    }
}
