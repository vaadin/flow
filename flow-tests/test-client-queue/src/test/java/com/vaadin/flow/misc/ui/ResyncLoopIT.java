/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import java.util.List;
import java.util.logging.Level;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.logging.LogEntry;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ResyncLoopIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/resync-loop";
    }

    @Test
    public void backgroundAccessSynchronouslyOnAttach_pushMessageOnResync_resyncSucceed() {
        open();
        waitForViewFullyLoaded();

        $(NativeButtonElement.class).id(ResyncLoopView.FORCE_RESYNC).click();

        assertThatResyncSucceed();
    }

    @Test
    public void backgroundAccessSynchronouslyOnAttach_interactDuringResync_resyncSucceed() {
        open();
        waitForViewFullyLoaded();

        $(NativeButtonElement.class).id(ResyncLoopView.FORCE_RESYNC).click();

        NativeButtonElement uselessButton = $(NativeButtonElement.class)
                .id(ResyncLoopView.USELESS_BUTTON);
        uselessButton.click();
        uselessButton.click();
        uselessButton.click();
        uselessButton.click();

        assertThatResyncSucceed();
    }

    @Test
    public void backgroundAccessOnAttach_pushMessageOnResync_resyncSucceed() {
        open();
        waitForViewFullyLoaded();

        $(NativeButtonElement.class).id(ResyncLoopView.ACCESS_MODE_ACCESS)
                .click();
        waitUntil(d -> $(SpanElement.class).id(ResyncLoopView.ACCESS_MODE)
                .getText().endsWith(": access"));

        $(NativeButtonElement.class).id(ResyncLoopView.FORCE_RESYNC).click();

        assertThatResyncSucceed();
    }

    @Test
    public void backgroundAccessOnAttach_interactDuringResync_resyncSucceed() {
        open();
        waitForViewFullyLoaded();

        $(NativeButtonElement.class).id(ResyncLoopView.ACCESS_MODE_ACCESS)
                .click();
        waitUntil(d -> $(SpanElement.class).id(ResyncLoopView.ACCESS_MODE)
                .getText().endsWith(": access"));

        $(NativeButtonElement.class).id(ResyncLoopView.FORCE_RESYNC).click();

        NativeButtonElement uselessButton = $(NativeButtonElement.class)
                .id(ResyncLoopView.USELESS_BUTTON);
        uselessButton.click();
        uselessButton.click();
        uselessButton.click();
        uselessButton.click();

        assertThatResyncSucceed();
    }

    private void waitForViewFullyLoaded() {
        waitUntil(driver -> $(SpanElement.class).id(ResyncLoopView.JS_CALLBACK)
                .getText().endsWith(": 1"));
        waitUntil(driver -> $(SpanElement.class).id(ResyncLoopView.BG_CALLBACK)
                .getText().endsWith(": 1"));
    }

    private void assertThatResyncSucceed() {

        waitUntil(driver -> $(SpanElement.class).id(ResyncLoopView.JS_CALLBACK)
                .getText().endsWith(": 2"));
        waitUntil(driver -> $(SpanElement.class).id(ResyncLoopView.BG_CALLBACK)
                .getText().endsWith(": 2"));

        List<String> logEntries = getLogEntries(Level.WARNING).stream()
                .map(LogEntry::getMessage).toList();
        Assert.assertTrue("Expecting lost message, but was not reported",
                logEntries.stream().anyMatch(message -> message.matches(
                        ".*Gave up waiting for message \\d+ from the server.*")));
        Assert.assertTrue(
                "Expecting resynchronizing from server message, but was not reported",
                logEntries.stream().anyMatch(message -> message
                        .matches(".*Resynchronizing from server.*")));
    }
}
