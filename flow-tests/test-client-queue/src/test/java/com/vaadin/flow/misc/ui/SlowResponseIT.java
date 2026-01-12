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
package com.vaadin.flow.misc.ui;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.misc.ui.SlowResponseView.ADD;
import static com.vaadin.flow.misc.ui.SlowResponseView.ADDED_PREDICATE;
import static com.vaadin.flow.misc.ui.SlowResponseView.SLOW_ADD;

public class SlowResponseIT extends ChromeBrowserTest {

    private static final Predicate<String> DUPLICATE_RESPONSE_LOG_MESSAGE_PREDICATE = Pattern
            .compile(
                    ".*Received message with server id \\d but have already seen \\d. Ignoring it.*")
            .asMatchPredicate();

    @Override
    protected String getTestPath() {
        return "/slow-response";
    }

    @Test
    public void slowResponseForRequest_clientDontResendsRequest_serverAnswersCorrectly() {
        open();

        try {
            waitUntil(driver -> $(NativeButtonElement.class).withId(SLOW_ADD)
                    .exists());
        } catch (TimeoutException te) {
            Assert.fail("Expected 'slow add element' button wasn't found");
        }
        // Add element normally
        $(NativeButtonElement.class).id(ADD).click();
        Assert.assertTrue(
                $(DivElement.class).id(ADDED_PREDICATE + 0).isDisplayed());

        // Request null response for next add
        $(NativeButtonElement.class).id(SLOW_ADD).click();

        $(NativeButtonElement.class).id(ADD).click();

        try {
            waitUntil(driver -> $(DivElement.class).withId(ADDED_PREDICATE + 1)
                    .exists());
        } catch (TimeoutException te) {
            Assert.fail(
                    "New element was not added though client should re-send request.");
        }

        Assert.assertTrue(
                "Slow response click message sent multiple times and got duplicate response",
                getLogEntries(Level.WARNING).stream().noneMatch(
                        logEntry -> DUPLICATE_RESPONSE_LOG_MESSAGE_PREDICATE
                                .test(logEntry.getMessage())));
    }

    @Test
    public void clickWhileRequestPending_clientQueuesRequests_messagesSentCorrectly() {
        open();

        try {
            waitUntil(driver -> $(NativeButtonElement.class).withId(SLOW_ADD)
                    .exists());
        } catch (TimeoutException te) {
            Assert.fail("Expected 'slow add element' button wasn't found");
        }

        // Add element normally
        $(NativeButtonElement.class).id(ADD).click();
        Assert.assertTrue(
                $(DivElement.class).id(ADDED_PREDICATE + 0).isDisplayed());

        // Request null response for next add
        $(NativeButtonElement.class).id(SLOW_ADD).click();

        $(NativeButtonElement.class).id(ADD).click();
        $(NativeButtonElement.class).id(ADD).click();

        try {
            waitUntil(driver -> $(DivElement.class).withId(ADDED_PREDICATE + 1)
                    .exists());
        } catch (TimeoutException te) {
            Assert.fail(
                    "New element was not added though client should re-send request.");
        }

        try {
            waitUntil(driver -> $(DivElement.class).withId(ADDED_PREDICATE + 2)
                    .exists());
        } catch (TimeoutException te) {
            Assert.fail(
                    "Second new element was not added though client should queue request.");
        }

        Assert.assertTrue(
                "Slow response click message sent multiple times and got duplicate response",
                getLogEntries(Level.WARNING).stream().noneMatch(
                        logEntry -> DUPLICATE_RESPONSE_LOG_MESSAGE_PREDICATE
                                .test(logEntry.getMessage())));

    }
}
