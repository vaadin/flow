/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class TrackMessageSizeIT extends ChromeBrowserTest {

    @Test
    public void runTests() {
        open();
        Assert.assertEquals("All tests run",
                this.findElement(By.id("logId")).getText());
    }
}
