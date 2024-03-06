/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * This test only works in DevMode since it needs client logging enabled.
 */
public class TimingInfoReportedIT extends ChromeBrowserTest {
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
