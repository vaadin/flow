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
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Test;

import org.junit.Before;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.flow.testutil.ChromeDeviceTest;

public class PushComponentIT extends ChromeDeviceTest {
    @Before
    public void init() {
        getDriver().get(getRootURL());
        waitForDevServer();
        getDriver().get(getRootURL() + "/push-component.html");
    }

    @Test
    public void componentGetsPushUpdates() {
        int initialCount = getCount();
        Assert.assertTrue(
                "The initial count should be less than maximum 50, but it has value "
                        + initialCount,
                initialCount < 50);

        waitUntil(driver -> getCount() > initialCount, 5);

        int nextCount = getCount();
        Assert.assertTrue(
                "The next count should be less than maximum 50, but it has value "
                        + nextCount,
                nextCount < 50);

        waitUntil(driver -> getCount() == 50, 5);

        int lastCount = getCount();
        Assert.assertEquals(
                "The update count should have reached the maximum 50, but it "
                        + "has value " + lastCount,
                50, lastCount);
    }

    private int getCount() {
        TestBenchElement component = $("push-component").first();
        String count = component.getText();
        return Integer.parseInt(count);
    }
}
