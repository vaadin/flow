/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Category(IgnoreOSGi.class)
public class ServiceInitListenersIT extends ChromeBrowserTest {

    @Test
    public void testServiceInitListenerTriggered() {
        open();

        List<WebElement> labels = findElements(By.tagName("label"));
        Assert.assertNotEquals(labels.get(0).getText(), 0,
                extractCount(labels.get(0).getText()));
        Assert.assertNotEquals(labels.get(1).getText(), 0,
                extractCount(labels.get(1).getText()));
        Assert.assertNotEquals(labels.get(2).getText(), 0,
                extractCount(labels.get(2).getText()));
    }

    private int extractCount(String logRow) {
        // Assuming row pattern is "label: 1"
        String substring = logRow.replaceAll("[^:]*:\\s*", "");
        return Integer.parseInt(substring);
    }
}
