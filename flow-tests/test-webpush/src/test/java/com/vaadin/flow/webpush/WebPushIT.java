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
package com.vaadin.flow.webpush;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.webpush.WebPushView.CHECK_ID;
import static com.vaadin.flow.webpush.WebPushView.EVENT_LOG_ID;
import static com.vaadin.flow.webpush.WebPushView.NOTIFY_ID;
import static com.vaadin.flow.webpush.WebPushView.SUBSCRIBE_ID;
import static com.vaadin.flow.webpush.WebPushView.UNSUBSCRIBE_ID;

public class WebPushIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Override
    protected void updateHeadlessChromeOptions(ChromeOptions chromeOptions) {
        // Create prefs map to store all preferences
        Map<String, Object> prefs = new HashMap<>();

        // Put this into prefs map to switch off browser notification
        prefs.put("profile.default_content_setting_values.notifications", 1);
        chromeOptions.setExperimentalOption("prefs", prefs);
    }

    @Override
    public void setDesiredCapabilities(
            DesiredCapabilities desiredCapabilities) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(String.format(
                "--unsafely-treat-insecure-origin-as-secure=%s", getRootURL()));
        opts.addArguments("--disable-dev-shm-usage");

        updateHeadlessChromeOptions(opts);

        desiredCapabilities.merge(opts);
        super.setDesiredCapabilities(desiredCapabilities);
    }

    @After
    public void cleanup() {

        // Request remove subscription always after test.
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript(
                """
                        if(navigator.serviceWorker) {
                          const registration = await navigator.serviceWorker.getRegistration();
                          const subscription = await registration?.pushManager.getSubscription();
                          if (subscription) {
                            await subscription.unsubscribe();
                          }
                        }
                        return true;
                        """);
    }

    @Test
    public void testServletDeployed() {
        open();

        JavascriptExecutor jse = (JavascriptExecutor) driver;
        Assert.assertFalse("WebPush should not be automatically loaded.",
                (boolean) jse.executeScript(
                        "if(window.Vaadin.Flow.webPush){return true;} return false;"));

        Assert.assertTrue("No service worker initiated",
                (boolean) jse.executeScript(
                        "if(navigator.serviceWorker)return true;return false;"));

        NativeButtonElement checkButton = $(NativeButtonElement.class)
                .id(CHECK_ID);
        checkButton.click();

        DivElement eventLog = $(DivElement.class).id(EVENT_LOG_ID);

        waitUntil(driver -> eventLog.$(DivElement.class).all().size() == 1);
        Assert.assertEquals("", 1, eventLog.$(DivElement.class).all().size());

        Assert.assertTrue("WebPush should be loaded with first call to API.",
                Boolean.valueOf((String) jse.executeScript(
                        "if(window.Vaadin.Flow.webPush){return 'true';} return 'false';")));

        Assert.assertEquals("No subscription should exist",
                "1: Subscription false",
                eventLog.$(DivElement.class).id("event-1").getText());

        NativeButtonElement subscribe = $(NativeButtonElement.class)
                .id(SUBSCRIBE_ID);
        try {
            subscribe.click();

            waitUntil(driver -> eventLog.$(DivElement.class).all().size() >= 2,
                    60);

            Assert.assertEquals("Subscription should be logged", 2,
                    eventLog.$(DivElement.class).all().size());
            Assert.assertTrue("", eventLog.$(DivElement.class).id("event-2")
                    .getText().startsWith("2: Subscribed "));

            $(NativeButtonElement.class).id(NOTIFY_ID).click();

            Assert.assertEquals("Subscription should be logged", 3,
                    eventLog.$(DivElement.class).all().size());
            Assert.assertTrue("", eventLog.$(DivElement.class).id("event-3")
                    .getText().equals("3: Sent notification"));

            waitUntil(driver -> isNotificationPresent(driver));
        } finally {
            $(NativeButtonElement.class).id(UNSUBSCRIBE_ID).click();
        }
        Assert.assertEquals("Unsubscribe should be logged", 4,
                eventLog.$(DivElement.class).all().size());
        Assert.assertTrue("", eventLog.$(DivElement.class).id("event-4")
                .getText().startsWith("4: Unsubscribed "));
    }

    public boolean isNotificationPresent(WebDriver driver) {
        return (boolean) ((JavascriptExecutor) driver).executeScript(
                """
                        return await navigator.serviceWorker.getRegistration().then( (ev) => ev.getNotifications() )
                            .then( (notifications) => {
                                return notifications.length == 1 &&
                                    notifications[0].title === 'Test title' &&
                                    notifications[0].body === 'Testing notification' &&
                                    notifications[0].badge === 'https://upload.wikimedia.org/wikipedia/commons/0/0e/Message-icon-blue-symbol-double.png' &&
                                    notifications[0].data === 'This is my data!' &&
                                    notifications[0].dir === 'rtl' &&
                                    notifications[0].icon === 'https://upload.wikimedia.org/wikipedia/commons/0/0e/Message-icon-blue-symbol-double.png' &&
                                    notifications[0].lang === 'de-DE' &&
                                    notifications[0].renotify === true &&
                                    notifications[0].requireInteraction === true &&
                                    notifications[0].silent === false &&
                                    notifications[0].tag === 'My Notification' &&
                                    Array.isArray(notifications[0].actions) && notifications[0].actions.length > 0 && notifications[0].actions[0].action === 'dashboard' &&
                                    Array.isArray(notifications[0].vibrate) && notifications[0].vibrate.length > 0 && notifications[0].vibrate[0] === 500;
                            });
                        """);
    }
}
