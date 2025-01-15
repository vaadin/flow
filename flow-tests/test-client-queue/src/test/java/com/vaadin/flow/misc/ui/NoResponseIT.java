package com.vaadin.flow.misc.ui;

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.misc.ui.TestNoResponseView.ADD;
import static com.vaadin.flow.misc.ui.TestNoResponseView.ADDED_PREDICATE;
import static com.vaadin.flow.misc.ui.TestNoResponseView.DELAY_NEXT_RESPONSE;

public class NoResponseIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/no-response";
    }

    @Test
    public void noResponseForRequest_clientResendsRequest_serverAnswersCorrectly() {
        open();

        try {
            waitUntil(driver -> $(NativeButtonElement.class)
                    .withId(DELAY_NEXT_RESPONSE).exists());
        } catch (TimeoutException te) {
            Assert.fail("Expected 'delay next' button wasn't found");
        }

        // Add element normally
        $(NativeButtonElement.class).id(ADD).click();
        Assert.assertTrue(
                $(DivElement.class).id(ADDED_PREDICATE + 0).isDisplayed());

        // Request null response for next add
        $(NativeButtonElement.class).id(DELAY_NEXT_RESPONSE).click();

        $(NativeButtonElement.class).id(ADD).click();

        Assert.assertEquals("No expected empty response found", 1,
                getLogEntries(Level.WARNING).stream()
                        .filter(logEntry -> logEntry.getMessage().contains(
                                "Response didn't contain a server id."))
                        .count());

        try {
            waitUntil(driver -> $(DivElement.class).withId(ADDED_PREDICATE + 1)
                    .exists());
        } catch (TimeoutException te) {
            Assert.fail(
                    "New element was not added though client should re-send request.");
        }

    }

    @Test
    public void clickWhileRequestPending_clientQueuesRequests_messagesSentCorrectly() {
        open();

        try {
            waitUntil(driver -> $(NativeButtonElement.class)
                    .withId(DELAY_NEXT_RESPONSE).exists());
        } catch (TimeoutException te) {
            Assert.fail("Expected 'delay next' button wasn't found");
        }

        // Add element normally
        $(NativeButtonElement.class).id(ADD).click();
        Assert.assertTrue(
                $(DivElement.class).id(ADDED_PREDICATE + 0).isDisplayed());

        // Request null response for next add
        $(NativeButtonElement.class).id(DELAY_NEXT_RESPONSE).click();

        $(NativeButtonElement.class).id(ADD).click();
        $(NativeButtonElement.class).id(ADD).click();

        Assert.assertEquals("No expected empty response found", 1,
                getLogEntries(Level.WARNING).stream()
                        .filter(logEntry -> logEntry.getMessage().contains(
                                "Response didn't contain a server id."))
                        .count());

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

    }
}
