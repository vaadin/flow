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
package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

/**
 * @author Vaadin Ltd
 *
 */
public class TimingInfoReportedIT extends PhantomJSTest {

    @Test
    public void ensureTimingsAvailable() {
        // The very first request can contain 0 as
        // CumulativeRequestDuration and -1 as CumulativeRequestDuration
        open();
        // Check timings starting from the second request
        open();

        Assert.assertEquals(1, findElements(By.className("log")).size());
        Assert.assertEquals("Timings ok",
                findElement(By.className("log")).getText());

    }
}
