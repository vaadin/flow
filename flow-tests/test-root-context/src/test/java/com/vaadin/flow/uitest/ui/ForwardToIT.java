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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ForwardToIT extends ChromeBrowserTest {

    @Test
    public void testForwardingToView() {
        String initUrl = getDriver().getCurrentUrl();
        open();

        Assert.assertTrue("should forward to specified view",
                findElement(By.id("root")).isDisplayed());
        Assert.assertTrue("should update update the URL",
                getDriver().getCurrentUrl().endsWith(
                        "com.vaadin.flow.uitest.ui.BasicComponentView"));

        getDriver().navigate().back();
        Assert.assertEquals("should replace history state",
                getDriver().getCurrentUrl(), initUrl);
    }
}
