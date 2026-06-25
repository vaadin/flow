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
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NavigationEventsIT extends ChromeBrowserTest {

    @Test
    public void assertNavigationToSelfProducesNavigationEvents() {
        open();

        // Initially there should be one round of navigation events
        assertMessages(2);

        // RouterLink click should cause second set of events
        $(AnchorElement.class).id("router-link").click();
        assertMessages(4);

        // Anchor click should cause third set of events
        $(AnchorElement.class).id("anchor").click();
        assertMessages(6);
    }

    private void assertMessages(int expectedSize) {
        List<String> messages = getMessages();
        Assert.assertEquals("Unexpected amount of navigation events",
                expectedSize, messages.size());
        Assert.assertEquals("Second to last event should be BeforeEnter",
                NavigationEventsView.BEFORE_ENTER,
                messages.get(expectedSize - 2));
        Assert.assertEquals("Last event should be AfterNavigation",
                NavigationEventsView.AFTER_NAVIGATION,
                messages.get(expectedSize - 1));

    }

    private List<String> getMessages() {
        return findElement(By.id("messages"))
                .findElements(By.cssSelector("div")).stream()
                .map(WebElement::getText).collect(Collectors.toList());
    }
}
