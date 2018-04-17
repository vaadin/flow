/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DomEventFilterIT extends ChromeBrowserTest {

    @Test
    public void filtering() {
        open();

        WebElement input = findElement(By.id("space"));

        input.sendKeys("asdf");

        Assert.assertEquals(0, getMessages().size());

        input.sendKeys("foo  bar");

        Assert.assertEquals(2, getMessages().size());
    }

    @Test
    public void debounce() throws InterruptedException {
        open();

        WebElement input = findElement(By.id("debounce"));

        input.sendKeys("a");
        assertMessages(0, "Leading: a", "Throttle: a");

        // Halfway into the idle interval
        Thread.sleep(500);
        input.sendKeys("b");

        /*
         * Wait until t = 1250: halfway between t = 1000 when the throttle
         * triggers and t = 1500 when the idle triggers
         */
        Thread.sleep(750);

        assertMessages(2, "Throttle: ab");

        /*
         * Wait until t = 1750: halfway between t = 1500 when the idle triggers
         * and t = 2000 when a new throttle would trigger
         */
        Thread.sleep(500);
        assertMessages(3, "Trailing: ab");

        /*
         * Wait until t = 2250: 250 beyond 2000 when a new throttle would
         * trigger if the idle hadn't triggered
         */
        Thread.sleep(500);
        assertMessages(4);
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
