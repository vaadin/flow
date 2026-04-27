/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class GeolocationIT extends ChromeBrowserTest {

    @Test
    public void get_returnsPosition() {
        open();
        findElement(By.id("getButton")).click();
        waitUntil(d -> findElement(By.id("getResult")));
        WebElement result = findElement(By.id("getResult"));
        Assert.assertEquals("lat=60.1699, lon=24.9384", result.getText());
    }

    @Test
    public void get_returnsError() {
        open();
        findElement(By.id("getErrorButton")).click();
        waitUntil(d -> findElement(By.id("getErrorResult")));
        WebElement result = findElement(By.id("getErrorResult"));
        Assert.assertTrue(
                "Expected error text to start with 'error=PERMISSION_DENIED:' "
                        + "but was: " + result.getText(),
                result.getText().startsWith("error=PERMISSION_DENIED:"));
    }

    @Test
    public void track_receivesPositionUpdatesUntilStopped() {
        open();
        findElement(By.id("trackButton")).click();

        // Wait for at least two position updates to confirm the mock is
        // streaming positions via setInterval and the tracker re-renders on
        // every one.
        waitUntil(d -> findElement(By.id("trackResult2")));
        Assert.assertEquals("lat=60.1699, lon=24.9384",
                findElement(By.id("trackResult1")).getText());
        Assert.assertEquals("lat=60.1699, lon=24.9384",
                findElement(By.id("trackResult2")).getText());

        // Stop tracking and snapshot how many updates had been rendered by
        // the time stop() returned. A position event that was already
        // in-flight when stop() ran can still land after stopResult appears,
        // so sleep briefly before reading the count to let any such event
        // settle — without this guard the assertion below can flake.
        findElement(By.id("stopButton")).click();
        waitUntil(d -> findElement(By.id("stopResult")));
        sleep(100);
        int countAtStop = findElements(By.cssSelector("[id^='trackResult']"))
                .size();

        // After stop(), the mock's setInterval is still running (it is only
        // cleared by clearWatch, which the tracker posts on stop). Give the
        // server a full second to confirm no further trackResult divs are
        // appended.
        sleep(1000);
        int countAfterWait = findElements(By.cssSelector("[id^='trackResult']"))
                .size();
        Assert.assertEquals(
                "No new trackResult divs should appear after stop()",
                countAtStop, countAfterWait);

        // Verify the div index didn't silently advance either (i.e. no
        // orphan trackResultN+1 was rendered out of view).
        try {
            findElement(By.id("trackResult" + (countAtStop + 1)));
            Assert.fail("Unexpected trackResult" + (countAtStop + 1)
                    + " rendered after stop()");
        } catch (NoSuchElementException expected) {
            // expected
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
