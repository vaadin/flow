/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.BrowserUtil;

public class CountUIsIT extends ChromeBrowserTest {

    @Test
    public void countUisNumer_onlyOneUIShouldBeInitiialized() {
        if (!BrowserUtil.isChrome(getDesiredCapabilities())) {
            // limit this test for being executed in one browser only
            return;
        }
        open();

        $(NativeButtonElement.class).first().click();

        WebElement uisCount = findElement(By.id("uis"));
        int count = Integer.parseInt(uisCount.getText());

        // there should not be any UI instance which is created but never has
        // been navigated (no any enter event into a navigation target)
        Assert.assertEquals(0, count);
    }
}
