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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BrowserWindowResizeIT extends ChromeBrowserTest {

    @Test
    public void listenResizeEvent() throws InterruptedException {
        open();

        Dimension currentSize = getDriver().manage().window().getSize();

        int newWidth = currentSize.getWidth() - 10;
        int newHeight = currentSize.getHeight() - 10;
        getDriver().manage().window()
                .setSize(new Dimension(newWidth, newHeight));

        // debounced by default with 300
        Thread.sleep(500);

        WebElement info = findElement(By.id("size-info"));

        Assert.assertEquals(String.valueOf(newWidth),
                info.getText().split("x")[0]);
        // Selenium sets the window size, the tested API reports viewport
        // size...
        int actualHeight = Integer.parseInt(info.getText().split("x")[1]);

        newWidth -= 30;
        newHeight -= 20;
        actualHeight -= 20;
        getDriver().manage().window()
                .setSize(new Dimension(newWidth, newHeight));

        Thread.sleep(500);

        Assert.assertEquals(String.valueOf(newWidth),
                info.getText().split("x")[0]);
        Assert.assertEquals(String.valueOf(actualHeight),
                info.getText().split("x")[1]);

        // check the same comes in still if modal component set
        findElement(By.id("modal")).click();
        newWidth += 30;
        newHeight += 20;
        actualHeight += 20;
        getDriver().manage().window()
                .setSize(new Dimension(newWidth, newHeight));

        Thread.sleep(500);

        Assert.assertEquals(String.valueOf(newWidth),
                info.getText().split("x")[0]);
        Assert.assertEquals(String.valueOf(actualHeight),
                info.getText().split("x")[1]);
    }
}
