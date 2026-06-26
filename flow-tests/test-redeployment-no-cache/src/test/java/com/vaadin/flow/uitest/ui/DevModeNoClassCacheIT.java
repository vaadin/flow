/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class DevModeNoClassCacheIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return super.getTestPath().replace("/view", "");
    }

    @Test
    public void testDevModeClassCacheNotPopulated() {
        open();

        waitForElementPresent(By.id("last-span"));

        List<TestBenchElement> allSpans = $("span").all();

        for (int i = 0; i < 5; i++) {
            String[] value = allSpans.get(i).getText().split(":");
            Assert.assertEquals("Expected " + value[0] + " to be 0.", 0,
                    Integer.parseInt(value[1]));
        }
    }
}
