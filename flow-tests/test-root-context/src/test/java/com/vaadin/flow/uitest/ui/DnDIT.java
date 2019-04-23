/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

/**
 * This testing is using a mock of browser html5 dnd support just for fun
 * because the headless chrome doesn't support html5 dnd:
 * https://bugs.chromium.org/p/chromedriver/issues/detail?id=2695
 */
public class DnDIT extends ChromeBrowserTest {

    @Test
    public void testCopyEffectElement_droppedToAllLanes() {
        open();

        TestBenchElement boxElement = getBoxElement("COPY");

        // not testing with the lane that does not have drop effect set, because
        // that just causes complex mocking logic and doesn't validate
        // anything else
        dragBoxToLanes(boxElement, getLaneElement("COPY"), true);
        dragBoxToLanes(boxElement, getLaneElement("MOVE"), false);
        dragBoxToLanes(boxElement, getLaneElement("LINK"), false);
        dragBoxToLanes(boxElement, getLaneElement("NONE"), false);
    }

    @Test
    public void testCopyEffectElement_droppedToDeactivatedLane_noDrop() {
        open();
        TestBenchElement boxElement = getBoxElement("COPY");

        dragBoxToLanes(boxElement, getLaneElement("COPY"), true);
        dragBoxToLanes(boxElement, getLaneElement("deactivated"), false);
    }

    private void dragBoxToLanes(TestBenchElement boxElement,
            TestBenchElement laneElement, boolean dropShouldOccur) {
        clearEvents();

        dragAndDrop(boxElement, laneElement);

        // need to wait for roundtrip, there should always be 2 events after dnd
        waitForElementPresent(By.id("event-" + (dropShouldOccur ? "3" : "2")));

        verifyStartEvent(1, boxElement);
        if (dropShouldOccur) {
            verifyDropEvent(2, boxElement, laneElement);
            verifyEndEvent(3, boxElement, laneElement);
        } else {
            verifyEndEvent(2, boxElement, null);
        }
    }

    private void verifyStartEvent(int i, TestBenchElement boxElement) {
        TestBenchElement eventlog = getEventlog(i);
        String expected = new StringBuilder().append(i).append(": Start: ")
                .append(boxElement.getText()).toString();
        Assert.assertEquals("Invalid start event details", expected,
                eventlog.getText());
    }

    private void verifyEndEvent(int i, TestBenchElement boxElement,
            TestBenchElement laneElement) {
        TestBenchElement eventlog = getEventlog(i);

        // dnd-simulation must hardcode replace a working drop effect when
        // nothing set. in reality, browser determines it based on effect
        // allowed, copy is the default if both are missing (Chrome)
        String dropEffect = laneElement == null ? "NONE"
                : laneElement.getText();

        String expected = new StringBuilder().append(i).append(": End: ")
                .append(boxElement.getText()).append(" ").append(dropEffect)
                .toString();
        Assert.assertEquals("Invalid end event details", expected,
                eventlog.getText());
    }

    private void verifyDropEvent(int i, TestBenchElement boxElement,
            TestBenchElement laneElement) {
        TestBenchElement eventlog = getEventlog(i);

        String effectAllowed = boxElement.getText();
        String dropEffect = laneElement.getText();

        String expected = new StringBuilder().append(i).append(": Drop: ")
                .append(effectAllowed).append(" ").append(dropEffect)
                .toString();
        Assert.assertEquals("Invalid drop event details", expected,
                eventlog.getText());
    }

    private TestBenchElement getEventlog(int i) {
        return $(TestBenchElement.class).id("event-" + i);
    }

    private void clearEvents() {
        findElement(By.tagName("button")).click();
    }

    private TestBenchElement getLaneElement(String dropEffect) {
        return $(TestBenchElement.class).id("lane-" + dropEffect);
    }

    private TestBenchElement getBoxElement(String effectAllowed) {
        return $(TestBenchElement.class).id("box-" + effectAllowed);
    }

}
