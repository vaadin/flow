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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * Integration test for {@link DnDAbsolutePositioningView} verifying that
 * dragging the brick element moves it using clientX/clientY coordinates.
 */
public class DnDAbsolutePositioningIT extends ChromeBrowserTest {

    @Test
    public void dragBrick_brickMoves() {
        open();

        WebElement brick = findBrick();
        WebElement canvas = findCanvas();

        // Get initial position
        String initialLeft = brick.getCssValue("left");
        String initialTop = brick.getCssValue("top");

        // Drag the brick to a new location within the canvas
        Actions actions = new Actions(getDriver());
        actions.clickAndHold(brick).moveByOffset(50, 30).release().perform();

        // Wait for server round-trip
        waitForLogEntry();

        // Get new position
        String newLeft = brick.getCssValue("left");
        String newTop = brick.getCssValue("top");

        // Verify position changed
        Assert.assertNotEquals("Left position should have changed", initialLeft,
                newLeft);
        Assert.assertNotEquals("Top position should have changed", initialTop,
                newTop);
    }

    @Test
    public void dragBrick_logShowsMovement() {
        open();

        WebElement brick = findBrick();

        // Drag the brick
        Actions actions = new Actions(getDriver());
        actions.clickAndHold(brick).moveByOffset(100, 75).release().perform();

        // Wait for and verify log entry
        WebElement logEntry = waitForLogEntry();
        String logText = logEntry.getText();

        Assert.assertTrue(
                "Log should show pixel movement (got: " + logText + ")",
                logText.contains("Pixels moved"));
        Assert.assertTrue("Log should contain x value", logText.contains("x:"));
        Assert.assertTrue("Log should contain y value", logText.contains("y:"));
    }

    @Test
    public void dragBrick_multipleDrags_positionAccumulates() {
        open();

        WebElement brick = findBrick();

        Actions actions = new Actions(getDriver());

        // First drag
        actions.clickAndHold(brick).moveByOffset(20, 20).release().perform();
        waitForLogEntry();

        int leftAfterFirst = parsePixelValue(brick.getCssValue("left"));
        int topAfterFirst = parsePixelValue(brick.getCssValue("top"));

        // Second drag
        actions.clickAndHold(brick).moveByOffset(30, 40).release().perform();
        waitForLogEntryCount(2);

        int leftAfterSecond = parsePixelValue(brick.getCssValue("left"));
        int topAfterSecond = parsePixelValue(brick.getCssValue("top"));

        // Position should have increased further
        Assert.assertTrue(
                "Left should increase after second drag (first: "
                        + leftAfterFirst + ", second: " + leftAfterSecond + ")",
                leftAfterSecond > leftAfterFirst);
        Assert.assertTrue(
                "Top should increase after second drag (first: " + topAfterFirst
                        + ", second: " + topAfterSecond + ")",
                topAfterSecond > topAfterFirst);
    }

    private WebElement findBrick() {
        return findElement(By.xpath("//div[text()='DragMe']"));
    }

    private WebElement findCanvas() {
        // Canvas has lightyellow background
        return findElement(By.xpath("//div[contains(@style, 'lightyellow')]"));
    }

    private WebElement waitForLogEntry() {
        waitUntil(driver -> !driver.findElements(By.tagName("pre")).isEmpty());
        return findElement(By.tagName("pre"));
    }

    private void waitForLogEntryCount(int count) {
        waitUntil(driver -> driver.findElements(By.tagName("pre"))
                .size() >= count);
    }

    private int parsePixelValue(String value) {
        if (value == null || value.isEmpty() || "auto".equals(value)) {
            return 0;
        }
        return (int) Double.parseDouble(value.replace("px", "").trim());
    }
}
