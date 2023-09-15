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

import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class DevModeClassCacheIT extends AbstractReloadIT {

    @Override
    protected String getTestPath() {
        return super.getTestPath().replace("/view", "");
    }

    @Test
    public void testDevModeClassCachePopulated() {
        open();

        waitForElementPresent(By.id("last-span"));

        reloadAndWait();

        waitForElementPresent(By.id("last-span"));

        List<TestBenchElement> allSpans = $("span").all();

        for (int i = 1; i < 6; i++) {
            String[] value = allSpans.get(i).getText().split(":");
            Assert.assertTrue("Expected " + value[0] + " to be greater than 0.",
                    Integer.parseInt(value[1]) > 0);
        }

        Assert.assertEquals("Unexpected cached route packages.",
                "com.vaadin.flow.uitest.ui",
                allSpans.get(6).getText().split(":")[1]);
    }
}
