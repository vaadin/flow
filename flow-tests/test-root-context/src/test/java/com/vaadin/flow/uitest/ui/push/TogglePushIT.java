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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.junit.Assert.assertFalse;

@Category({ PushTests.class, IgnoreOSGi.class })
public class TogglePushIT extends ChromeBrowserTest {

    @Test
    public void togglePushInInit() throws Exception {
        // Open with push disabled
        open("push=disabled");

        assertFalse(getPushToggle().isSelected());

        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);
        Assert.assertEquals("Counter has been updated 0 times",
                getCounterText());

        // Open with push enabled
        open("push=enabled");
        Assert.assertThat(getPushToggle().getText(),
                CoreMatchers.containsString("Push enabled"));

        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);
        Assert.assertEquals("Counter has been updated 1 times",
                getCounterText());

    }

    @Test
    public void togglePush() throws InterruptedException {
        open();
        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);

        // Push is enabled, so text gets updated
        Assert.assertEquals("Counter has been updated 1 times",
                getCounterText());

        // Disable push
        getPushToggle().click();
        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);
        // Push is disabled, so text is not updated
        Assert.assertEquals("Counter has been updated 1 times",
                getCounterText());

        getDirectCounterUpdateButton().click();
        // Direct update is visible, and includes previous update
        Assert.assertEquals("Counter has been updated 3 times",
                getCounterText());

        // Re-enable push
        getPushToggle().click();
        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);

        // Push is enabled again, so text gets updated
        Assert.assertEquals("Counter has been updated 4 times",
                getCounterText());
    }

    private WebElement getDirectCounterUpdateButton() {
        return findElement(By.id("update-counter"));
    }

    private WebElement getPushToggle() {
        return findElement(By.id("push-setting"));
    }

    private WebElement getDelayedCounterUpdateButton() {
        return findElement(By.id("update-counter-async"));
    }

    private String getCounterText() {
        return findElement(By.id("counter")).getText();
    }

}
