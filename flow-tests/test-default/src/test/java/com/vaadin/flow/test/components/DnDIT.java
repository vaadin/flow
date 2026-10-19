/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.components;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This testing is using a mock of browser html5 dnd support just for fun
 * because the headless chrome doesn't support html5 dnd:
 * https://bugs.chromium.org/p/chromedriver/issues/detail?id=2695
 */
@TestFor(DnDView.class)
public class DnDIT extends AbstractDefaultIT {

    @BrowserTest
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

    @BrowserTest
    public void testUndefinedEffectElement_allLanesGetDropTargetClass() {
        open();

        TestBenchElement boxElement = getBoxElement("no-effect");
        checkDragOverClassName(boxElement, getLaneElement("no-effect"));
        checkDragOverClassName(boxElement, getLaneElement("COPY"));
        checkDragOverClassName(boxElement, getLaneElement("MOVE"));
        checkDragOverClassName(boxElement, getLaneElement("LINK"));
    }

    @BrowserTest
    public void testCopyEffectElement_droppedToDeactivatedLane_noDrop() {
        open();
        TestBenchElement boxElement = getBoxElement("COPY");

        dragBoxToLanes(boxElement, getLaneElement("COPY"), true);
        dragBoxToLanes(boxElement, getLaneElement("deactivated"), false);
    }

    @BrowserTest
    public void testCopyEffectElement_disableElement_draggedNotPresent() {
        open();

        TestBenchElement boxElement = getBoxElement("COPY");
        clickElementWithJs("button-disable-enable-drag-sources");
        assertTrue(boxElement.hasAttribute("disabled"),
                "Invalid enabled state found in drag source");
        clearEvents();
        drag(boxElement);
        assertFalse(boxElement.hasClassName("v-dragged"));
    }

    @BrowserTest
    public void testCopyEffectElement_disableTarget_dragOverTargetNotPresent() {
        open();

        TestBenchElement boxElement = getBoxElement("COPY");
        TestBenchElement targetElement = getLaneElement("COPY");
        clickElementWithJs("button-disable-enable-drop-targets");
        assertTrue(targetElement.hasAttribute("disabled"),
                "Invalid enabled state found in drop target");
        dragElementOver(boxElement, targetElement);
        assertFalse(targetElement.hasClassName("v-drag-over-target"));
    }

    @BrowserTest
    public void testSetDragImage_withImage() {
        open();

        clickElementWithJs("button-toggle-drag-image-enabled");

        // effect could be anything, just testing the drag image.
        TestBenchElement boxElement = getBoxElement("COPY");
        clearEvents();

        drag(boxElement);

        waitForElementPresent(By.id("event-2"));

        TestBenchElement eventlog = getEventlog(2);
        assertDragImage(eventlog, "2: DragImage: <img ", ">", "alt=\"Gift\"",
                "src=\"/images/gift.png\"");
    }

    @BrowserTest
    public void testSetDragImage_imageIsClearedWithNull() {
        open();

        clickElementWithJs("button-toggle-drag-image-enabled");
        TestBenchElement boxElement = getBoxElement("COPY");
        TestBenchElement laneElement = getLaneElement("COPY");
        clearEvents();
        dragAndDrop(boxElement, laneElement);

        // clears drag image to null
        clickElementWithJs("button-toggle-drag-image-enabled");

        clearEvents();
        dragAndDrop(boxElement, laneElement);
        waitForElementPresent(By.id("event-3"));
        Assertions.assertEquals("1: Start: COPY", getEventlog(1).getText(),
                "Invalid event order");
        Assertions.assertEquals("2: Drop: COPY COPY", getEventlog(2).getText(),
                "Invalid event order");
    }

    @BrowserTest
    public void testSetDragImage_withVisibleComponentInViewport() {
        open();

        clickElementWithJs("button-toggle-drag-image-enabled");
        clickElementWithJs("button-toggle-image");

        TestBenchElement boxElement = getBoxElement("COPY");
        clearEvents();
        drag(boxElement);

        // need to wait for roundtrip, there should always be 3 events after dnd
        // with drag image
        waitForElementPresent(By.id("event-2"));

        TestBenchElement eventlog = getEventlog(2);
        String expected = "2: DragImage: <button id=\"button-toggle-image\">Toggle image</button>";
        Assertions.assertEquals(expected, eventlog.getText(),
                "Invalid drag image");
    }

    // visible component in viewport does not generate virtual element for drag
    // image.
    @BrowserTest
    public void testSetDragImage_visibleComponentInViewportIsClearedWithNull() {
        open();

        clickElementWithJs("button-toggle-drag-image-enabled");
        clickElementWithJs("button-toggle-image");
        TestBenchElement boxElement = getBoxElement("COPY");
        TestBenchElement laneElement = getLaneElement("COPY");
        clearEvents();
        dragAndDrop(boxElement, laneElement);

        // clears drag image to null
        clickElementWithJs("button-toggle-drag-image-enabled");

        clearEvents();
        dragAndDrop(boxElement, laneElement);
        waitForElementPresent(By.id("event-3"));
        Assertions.assertEquals("1: Start: COPY", getEventlog(1).getText(),
                "Invalid event order");
        Assertions.assertEquals("2: Drop: COPY COPY", getEventlog(2).getText(),
                "Invalid event order");
    }

    @BrowserTest
    public void testSetDragImage_withDragSourceAsDragImage() {
        open();

        clickElementWithJs("button-toggle-drag-image-enabled");
        clickElementWithJs("button-toggle-image");
        clickElementWithJs("button-toggle-image");

        TestBenchElement boxElement = getBoxElement("no-effect");
        clearEvents();
        drag(boxElement);

        // need to wait for roundtrip, there should always be 3 events after dnd
        // with drag image
        waitForElementPresent(By.id("event-2"));

        TestBenchElement eventlog = getEventlog(2);
        assertDragImage(eventlog,
                "2: DragImage: <div id=\"box-no-effect\" style=\"",
                "\">no-effect</div>", "width:100px", "border:1px solid",
                "margin:10px", "height:60px");
    }

    @BrowserTest
    public void testSetDragImage_withNotYetAttachedDragSource() {
        open();

        clickElementWithJs("button-add-drag-source-with-drag-image");

        TestBenchElement boxElement = $(TestBenchElement.class)
                .id("drag-source-with-image");
        clearEvents();
        drag(boxElement);

        waitForElementPresent(By.id("event-2"));
        TestBenchElement eventlog = getEventlog(2);
        assertDragImage(eventlog, "2: DragImage: <img ", ">", "alt=\"Gift\"",
                "src=\"/images/gift.png\"");
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
        Assertions.assertEquals(expected, eventlog.getText(),
                "Invalid start event details");
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
        Assertions.assertEquals(expected, eventlog.getText(),
                "Invalid end event details");
    }

    private void verifyDropEvent(int i, TestBenchElement boxElement,
            TestBenchElement laneElement) {
        TestBenchElement eventlog = getEventlog(i);

        String effectAllowed = boxElement.getText();
        String dropEffect = laneElement.getText();

        String expected = new StringBuilder().append(i).append(": Drop: ")
                .append(effectAllowed).append(" ").append(dropEffect)
                .toString();
        Assertions.assertEquals(expected, eventlog.getText(),
                "Invalid drop event details");
    }

    /**
     * Asserts the logged drag image HTML without depending on the order of
     * attributes or style declarations, which is not defined.
     */
    private void assertDragImage(TestBenchElement eventlog, String prefix,
            String suffix, String... fragments) {
        String text = eventlog.getText();
        Assertions.assertTrue(
                text.startsWith(prefix) && text.endsWith(suffix),
                "Invalid drag image: " + text);
        int expectedLength = prefix.length() + suffix.length();
        for (String fragment : fragments) {
            Assertions.assertTrue(text.contains(fragment),
                    "Invalid drag image: " + text);
            expectedLength += fragment.length();
        }
        // Separators: one between attributes or one per style declaration
        expectedLength += fragments.length - 1;
        Assertions.assertEquals(expectedLength, text.length(),
                "Invalid drag image: " + text);
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

    private void checkDragOverClassName(TestBenchElement dragged,
            TestBenchElement target) {
        dragElementOver(dragged, target);
        assertTrue(target.hasClassName("v-drag-over-target"));
    }

}
