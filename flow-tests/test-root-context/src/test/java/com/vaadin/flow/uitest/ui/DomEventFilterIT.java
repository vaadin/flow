/*
 * Copyright 2000-2025 Vaadin Ltd.
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

        /*
         * Note, the client side implementation "merges" all these settings in
         * the single "Debouncer" (per element-eventtype-timeout, not per server
         * side listener). This is probably an issue in the original
         * implementation that could maybe be refactored in some upcoming
         * release, but then we should create identifiers for server side
         * listeners and handle those somehow separately. My hunch is that it is
         * better just to document the limitations and focus on something that
         * actually benefits users. Today, real world use cases seem to be
         * somewhat in good shape.
         *
         * Thus, even though the element can be configured to have both trailing
         * and intermediate configured for the same element, for different
         * listeners, only one settings are currently supported. leading phase
         * can be configured with either.
         */
        WebElement debounce = findElement(By.id("debounce"));

        debounce.sendKeys("a");

        // Halfway into the idle interval, no event should be fired yet
        Thread.sleep(500);
        debounce.sendKeys("b");

        /*
         * Wait untill the idle timer fires
         */
        Thread.sleep(1250);

        int nextMsg = 0;

        assertMessages(nextMsg++, "input:ab, phase:TRAILING");

        debounce.sendKeys("c");
        debounce.click();

        // Ensure the server got both events, in correct order and immediately,
        // even though timer didn't yet fire for debouncing
        assertMessages(nextMsg++, "input:abc, phase:TRAILING", "click");
        nextMsg++; // two events came in one batch

        WebElement leading = findElement(By.id("leading"));

        leading.sendKeys("a");
        assertMessages(nextMsg++, "input:a, phase:LEADING");

        // new events should be ignored until the timeout
        leading.sendKeys("b");
        assertMessages(nextMsg);
        Thread.sleep(200);
        leading.sendKeys("c");
        assertMessages(nextMsg);

        Thread.sleep(1250);
        // even still should be just the original event reported
        assertMessages(nextMsg);

        // but now the beginning of "next burst" again should be synced right
        // away
        leading.sendKeys("d");
        assertMessages(nextMsg++, "input:abcd, phase:LEADING");

        // This element now has "throttling" ~ leading event and then somewhat
        // fixed rate. Still, if another event, like click, happens, the queue
        // should be emptied at that point.
        WebElement throttle = findElement(By.id("throttle"));

        throttle.sendKeys("a");
        assertMessages(nextMsg++, "input:a, phase:LEADING");

        /*
         * Wait untill the idle timer fires
         */
        Thread.sleep(2250);

        // There should be no new events, but burst should be considered "done"
        assertMessages(nextMsg);

        long burstStart = System.currentTimeMillis();
        // new leading should be triggered
        throttle.sendKeys("b");
        assertMessages(nextMsg++, "input:ab, phase:LEADING");

        // This should hold in queue and send together with the next after
        // 1000ms from the latest leading
        throttle.sendKeys("c");
        Thread.sleep(10);
        throttle.sendKeys("d");

        long millisToNextIntermediate = ((burstStart + 2000)
                - System.currentTimeMillis());

        Thread.sleep(millisToNextIntermediate + 100);

        // now only one event should have arrived
        assertMessages(nextMsg++, "input:abcd, phase:INTERMEDIATE");

        WebElement leadingTrailing = findElement(By.id("leading-trailing"));

        leadingTrailing.sendKeys("a");
        assertMessages(nextMsg++, "input:a, phase:LEADING"); // leading should
                                                             // come right away

        leadingTrailing.sendKeys("b");
        Thread.sleep(500);

        leadingTrailing.sendKeys("c");
        Thread.sleep(500);

        leadingTrailing.sendKeys("d");
        Thread.sleep(500);

        // only leading should still be reported to the server...
        assertMessages((nextMsg - 1), "input:a, phase:LEADING");

        // wait for the trailing event to land
        Thread.sleep(600);
        assertMessages(nextMsg++, "input:abcd, phase:TRAILING");

        /*
         * This is the special weirdomode, we get all the phases. As
         * INTERMEDIATE will practically always fire after TRAILING. We will
         * re-send the last event twice to server, the last just with different
         * phase.
         *
         * Might be in theor handy if actual events are not relevant, but the
         * activity time and still somewhat fresh content is needed during the
         * burst from time to time.
         *
         * The best would be to investigate if somebody is actually usign this
         * feature and remove if not (or rare). Makes the whole system very
         * complex and fragile (although not the only thing causing that).
         */
        WebElement godMode = findElement(By.id("godMode"));
        godMode.sendKeys("a");
        assertMessages(nextMsg++, "godmode:a, phase:LEADING");

        Thread.sleep(200);
        godMode.sendKeys("b");
        Thread.sleep(50);
        godMode.sendKeys("c");
        Thread.sleep(2000);
        assertMessages(nextMsg++, "godmode:abc, phase:INTERMEDIATE",
                "godmode:abc, phase:TRAILING");
        nextMsg++;

        WebElement twoEvents = findElement(By.id("twoEvents"));
        twoEvents.sendKeys("asdfg");
        Thread.sleep(5000);
        assertMessages(nextMsg++, "k-event 5.0 phase: TRAILING", "g-event 5.0");

    }

    @Test
    public void componentWithDebounce() throws InterruptedException {
        open();

        // note for maintainers:
        // sendkeys can be rather slow initially, especially in local
        // environments
        // Thus the test debounce latency is upped to 2000ms to avoid timing
        // issues

        WebElement input = findElement(By.id("debounce-component"));

        input.sendKeys("a");
        assertMessages(0);
        Thread.sleep(500);
        input.sendKeys("b");
        assertMessages(0);

        Thread.sleep(2001); // should be more than enough as the cmd itsel is
                            // slot

        assertMessages(0, "Component: ab");

        input.sendKeys("c");
        Thread.sleep(200);
        assertMessages(1);

        input.sendKeys("d");
        Thread.sleep(800);
        assertMessages(1);
        Thread.sleep(1200);
        assertMessages(1, "Component: abcd");

    }

    @Test
    public void twoListeners_removingOne_should_cleanItsFilter() {
        open();

        WebElement paragraph = findElement(By.id("result-paragraph"));
        WebElement button = findElement(By.id("listener-removal-button"));
        WebElement input = findElement(By.id("listener-input"));

        Assert.assertEquals("Result paragraph should be empty", "",
                paragraph.getText());

        input.sendKeys("a");
        Assert.assertEquals(
                "Filter should have prevented default, and input is empty", "",
                input.getAttribute("value"));
        Assert.assertEquals(
                "Event was sent to server and paragraph should be 'A'", "A",
                paragraph.getText());

        input.sendKeys("b");
        Assert.assertEquals(
                "Filter should have prevented default, and input is empty", "",
                input.getAttribute("value"));
        Assert.assertEquals(
                "Event was sent to server and paragraph should be 'B'", "B",
                paragraph.getText());

        // remove keybind for A
        button.click();
        Assert.assertEquals("Result paragraph should be 'REMOVED'", "REMOVED",
                paragraph.getText());

        // keybind for A should no longer work
        input.sendKeys("a");
        Assert.assertEquals("Filter should be removed, and input has 'a'", "a",
                input.getAttribute("value"));
        Assert.assertEquals("Result paragraph should still be 'REMOVED'",
                "REMOVED", paragraph.getText());

        // b should still be functional
        input.sendKeys("b");
        Assert.assertEquals(
                "Filter should have prevented default, and input has only 'a'",
                "a", input.getAttribute("value"));
        Assert.assertEquals(
                "Event was sent to server and paragraph should be 'B'", "B",
                paragraph.getText());
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
