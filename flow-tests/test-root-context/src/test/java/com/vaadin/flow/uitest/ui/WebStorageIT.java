/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WebStorageIT extends ChromeBrowserTest {

    @Test
    public void tetWebstorage() {
        open();

        WebElement input = findElement(By.id("input"));
        WebElement set = findElement(By.id("setText"));
        WebElement detect = findElement(By.id("detect"));
        WebElement remove = findElement(By.id("remove"));
        WebElement msg = findElement(By.id("msg"));

        input.clear();
        input.sendKeys("foobar");

        set.click();

        detect.click();

        String text = msg.getText();
        Assert.assertEquals("foobar", text);

        remove.click();

        text = msg.getText();
        Assert.assertEquals("", text);

    }
}
