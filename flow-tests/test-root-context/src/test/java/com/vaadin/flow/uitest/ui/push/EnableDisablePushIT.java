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
package com.vaadin.flow.uitest.ui.push;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category({ PushTests.class, IgnoreOSGi.class })
public class EnableDisablePushIT extends ChromeBrowserTest {
    @Test
    public void testEnablePushWhenUsingPolling() throws Exception {
        open();

        Assert.assertEquals("1. Push enabled", getLogRow(0));

        $(TestBenchElement.class).id("disable-push").click();
        Assert.assertEquals("3. Push disabled", getLogRow(2));

        $(TestBenchElement.class).id("enable-polling").click();
        Assert.assertEquals("5. Poll enabled", getLogRow(4));

        $(TestBenchElement.class).id("enable-push").click();
        Assert.assertEquals("7. Push enabled", getLogRow(6));

        $(TestBenchElement.class).id("disable-polling").click();
        Assert.assertEquals("9. Poll disabled", getLogRow(8));

        $(TestBenchElement.class).id("thread-re-enable-push").click();
        Thread.sleep(2500);
        Assert.assertEquals("16. Polling disabled, push enabled",
                getLogRow(15));

        $(TestBenchElement.class).id("disable-push").click();
        Assert.assertEquals("18. Push disabled", getLogRow(17));
    }

    private String getLogRow(int index) {
        return findElements(By.className("log")).get(index).getText();
    }

}
