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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class IFrameIT extends ChromeBrowserTest {

    @Test
    public void testIFrameReload() {
        open();

        waitForElementPresent(By.id("frame1"));
        getDriver().switchTo().frame("frame1");
        Assert.assertEquals("A", findElement(By.id("Friday")).getText());

        getDriver().switchTo().parentFrame();
        findElement(By.id("Reload")).click();

        getDriver().switchTo().frame("frame1");
        waitUntil(webDriver -> "B"
                .equals(findElement(By.id("Friday")).getText()));
    }

    @Test
    public void testIFrameWithDynamicResource() {
        open();

        waitForElementPresent(By.id("frame2"));
        getDriver().switchTo().frame("frame2");
        Assert.assertEquals("Dynamic", findElement(By.id("content")).getText());
    }
}
