/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * A test class for miscelaneous tests checking features or fixes that do not
 * require their own IT module.
 *
 * Adding new IT modules penalizes build time, otherwise appending tests to this
 * class run new tests faster.
 */
public class MiscelaneousIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/";
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        open();
    }

    @Test // #5964
    public void should_loadThemedComponent_fromLocal() {
        WebElement body = findElement(By.tagName("body"));
        Assert.assertEquals("2px", body.getCssValue("padding"));
    }

    /**
     * Checks that a missing or incorrect icon is handled properly with an error
     * log and does not halt the whole application startup.
     */
    @Test
    public void handlesIncorrectIconProperly() {
        open();

        checkLogsForErrors();

        Assert.assertTrue(
                "Missing/invalid icons at startup should be handled with error log.",
                isElementPresent(By.id(MiscelaneousView.TEST_VIEW_ID)));
    }
}
