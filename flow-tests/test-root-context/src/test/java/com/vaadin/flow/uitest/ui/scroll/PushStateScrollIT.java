/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PushStateScrollIT extends ChromeBrowserTest {
    @Test
    public void pushNoScroll() {
        testNoScrolling("push");
    }

    @Test
    public void replaceNoScroll() {
        testNoScrolling("replace");
    }

    private void testNoScrolling(String buttonId) {
        open();

        WebElement button = findElement(By.id(buttonId));

        scrollToElement(button);

        int scrollBeforeClick = getScrollY();

        // Sanity check
        Assert.assertNotEquals("Should be scrolled down before clicking", 0,
                scrollBeforeClick);

        button.click();

        Assert.assertEquals("Scroll position should not have changed",
                scrollBeforeClick, getScrollY());
    }
}
