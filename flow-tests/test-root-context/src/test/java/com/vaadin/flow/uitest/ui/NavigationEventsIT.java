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
