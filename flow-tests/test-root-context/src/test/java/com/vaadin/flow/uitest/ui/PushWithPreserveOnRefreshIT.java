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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreOSGi.class)
public class PushWithPreserveOnRefreshIT extends ChromeBrowserTest {

    @Test
    public void ensurePushWorksAfterRefresh() {
        open();

        WebElement loadingIndicator = findElement(
                By.className("v-loading-indicator"));

        waitUntil(driver -> !loadingIndicator.isDisplayed());

        TestBenchElement button = $(TestBenchElement.class).id("click");
        button.click();
        button.click();
        Assert.assertEquals("Button has been clicked 2 times", getLastLog());

        open();
        Assert.assertEquals("Button has been clicked 2 times", getLastLog());
        button = $(TestBenchElement.class).id("click");
        button.click();
        Assert.assertEquals("Button has been clicked 3 times", getLastLog());
    }

    private String getLastLog() {
        List<WebElement> logs = findElements(By.className("log"));
        if (logs.isEmpty()) {
            return null;
        }
        return logs.get(logs.size() - 1).getText();
    }
}
