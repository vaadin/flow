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

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DomEventStopPropagationAndPreventDefaultIT
        extends ChromeBrowserTest {

    @Test
    public void preventDefault() throws InterruptedException {
        open();

        int nextMsg = 0;

        WebElement btn = findElement(By.id("btn"));
        btn.click();
        // also event:div comes trhough if preventDefault does not work
        assertMessages(nextMsg++, "event:btn");

        WebElement component = findElement(By.id("component"));
        component.click();
        // There would be another message if the event was propagated
        // Should not be propagated because of the DomEvent config
        assertMessages(nextMsg++, "event:component");

    }

    private void assertMessages(int skip, String... expectedTail) {
        List<WebElement> messages = getMessages();
        if (messages.size() < skip) {
            Assert.fail("Cannot skip " + skip + " messages when there are only "
                    + messages.size() + "messages. " + joinMessages(messages));
        }

        messages = messages.subList(skip, messages.size());

        if (messages.size() < expectedTail.length) {
            Assert.fail("Expected " + expectedTail.length
                    + " messages, but there are only " + messages.size() + ". "
                    + joinMessages(messages));
        }

        for (int i = 0; i < expectedTail.length; i++) {
            Assert.assertEquals("Unexpected message at index " + i,
                    expectedTail[i], messages.get(i).getText());
        }

        if (messages.size() > expectedTail.length) {
            Assert.fail("There are unexpected messages at the end. "
                    + joinMessages(messages.subList(expectedTail.length,
                            messages.size())));
        }
    }

    private static String joinMessages(List<WebElement> messages) {
        return messages.stream().map(WebElement::getText)
                .collect(Collectors.joining("\n", "\n", ""));
    }

    private List<WebElement> getMessages() {
        WebElement messagesHolder = findElement(By.id("messages"));
        List<WebElement> messages = messagesHolder
                .findElements(By.cssSelector("div"));
        return messages;
    }
}
