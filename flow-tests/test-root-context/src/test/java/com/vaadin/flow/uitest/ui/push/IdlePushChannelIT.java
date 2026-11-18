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
package com.vaadin.flow.uitest.ui.push;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.testutil.PushConnectionTracker;

/**
 * Checks that push keeps working after the push connection has been idle for
 * longer than the server's idle timeouts, which close an idle websocket and
 * resume a suspended long polling request. The view is served by
 * {@link IdlePushChannelServlet}, which shortens those timeouts so that the
 * test does not have to wait for the five minute default.
 */
@Category(PushTests.class)
public abstract class IdlePushChannelIT extends ChromeBrowserTest {

    @Override
    protected void updateHeadlessChromeOptions(ChromeOptions chromeOptions) {
        PushConnectionTracker.enable(chromeOptions);
    }

    @Override
    protected String getTestPath() {
        return "/idle-push/" + URLEncoder.encode(getViewClass().getName(),
                StandardCharsets.UTF_8);
    }

    @Test
    public void longWaitBetweenActions() {
        open();
        BasicPushIT.getIncrementButton(this).click();
        testBench().disableWaitForVaadin();
        waitForClientCounter(1);
        PushConnectionTracker pushConnections = new PushConnectionTracker(
                getDriver());
        int attempts = pushConnections.getAttemptCount();

        // The idle timeout ends the push connection and the client opens a
        // new one; without it, this test would not test an idle timeout.
        // Reading the browser's network log does not touch the server, so
        // the connection stays idle meanwhile.
        try {
            waitUntil(driver -> pushConnections.getAttemptCount() > attempts,
                    10 * IdlePushChannelServlet.IDLE_TIMEOUT_MS / 1000);
        } catch (TimeoutException e) {
            Assert.fail("Push connection was not re-established while idle. "
                    + "Push connections: " + pushConnections.getTransports());
        }

        BasicPushIT.getIncrementButton(this).click();
        waitForClientCounter(2);

        // The server can still push to the client
        BasicPushIT.getServerCounterStartButton(this).click();
        try {
            waitUntil(driver -> BasicPushIT.getServerCounter(this) > 0);
        } catch (TimeoutException e) {
            Assert.fail("Server push did not reach the client after the idle "
                    + "period. Expected server counter > 0 but was "
                    + BasicPushIT.getServerCounter(this));
        }

        pushConnections.assertOnlyTransport(
                getViewClass().getAnnotation(CustomPush.class).transport());
    }

    private void waitForClientCounter(int expected) {
        try {
            waitUntil(driver -> BasicPushIT.getClientCounter(this) == expected);
        } catch (TimeoutException e) {
            Assert.fail("Expected client counter " + expected + " but was "
                    + BasicPushIT.getClientCounter(this));
        }
    }

}
