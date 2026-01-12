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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.Point;

/**
 * Integration test for {@link DnDAttachToDropLocationView} verifying that drop
 * coordinates (offsetX/offsetY) and drag start offsets work correctly.
 */
public class DnDAttachToDropLocationIT extends ChromeBrowserTest {

    @Test
    public void dragAndDrop_itemAppearsOnCanvas() {
        open();

        WebElement redItem = findDraggableByText("Red");
        WebElement canvas = findCanvas();

        // Use clickAndHold + moveToElement + release for better compatibility
        Actions actions = new Actions(getDriver());
        actions.clickAndHold(redItem).moveToElement(canvas).release().perform();

        // Wait for server round-trip
        waitUntilDroppedItemsCount(1);

        // Verify an item was created on the canvas
        List<WebElement> droppedItems = findDroppedItems();
        Assert.assertEquals("Should have one dropped item", 1,
                droppedItems.size());
        Assert.assertEquals("Item 1", droppedItems.get(0).getText());
    }

    @Test
    public void dragAndDrop_multipleItems_eachGetsUniqueNumber() {
        open();

        WebElement redItem = findDraggableByText("Red");
        WebElement greenItem = findDraggableByText("Green");
        WebElement blueItem = findDraggableByText("Blue");
        WebElement canvas = findCanvas();

        Actions actions = new Actions(getDriver());

        // Drop three items with waits between
        actions.clickAndHold(redItem).moveToElement(canvas).release().perform();
        waitUntilDroppedItemsCount(1);

        actions.clickAndHold(greenItem).moveToElement(canvas).release()
                .perform();
        waitUntilDroppedItemsCount(2);

        actions.clickAndHold(blueItem).moveToElement(canvas).release().perform();
        waitUntilDroppedItemsCount(3);

        // Verify three items were created
        List<WebElement> droppedItems = findDroppedItems();
        Assert.assertEquals("Should have three dropped items", 3,
                droppedItems.size());
    }

    @Test
    public void dragAndDrop_itemPositionedWithinCanvas() {
        open();

        final int startOffsetX = 1;
        final int startOffsetY = 2;
        
        final int offsetX = 40;
        final int offsetY = 30;
        WebElement redItem = findDraggableByText("Red");
        WebElement canvas = findCanvas();
        
        Point redItemTopLeft = redItem.getLocation();
        Point canvasTopLeft = canvas.getLocation();

        Actions actions = new Actions(getDriver());
        actions.moveToLocation(redItemTopLeft.getX() + startOffsetX, redItemTopLeft.getY() + startOffsetY)
                .clickAndHold()
                .moveToLocation(canvasTopLeft.getX() + offsetX, canvasTopLeft.getY() + offsetY)
                .release().perform();

        waitUntilDroppedItemsCount(1);

        // Find the dropped item
        WebElement droppedItem = findDroppedItems().get(0);

        // Verify the item has absolute positioning with left/top set
        String position = droppedItem.getCssValue("position");
        String left = droppedItem.getCssValue("left");
        String top = droppedItem.getCssValue("top");

        Assert.assertEquals("Item should have absolute positioning", "absolute",
                position);
        Assert.assertNotNull("Left should be set", left);
        Assert.assertNotNull("Top should be set", top);
        Assert.assertEquals("Left should match offsetx", (offsetX - startOffsetX) + "px", left);
        Assert.assertEquals("Top should match offsety", (offsetY - startOffsetY) + "px", top);
    }

    @Test
    public void dragAndDrop_droppedItemHasCorrectColor() {
        open();

        WebElement redItem = findDraggableByText("Red");
        WebElement canvas = findCanvas();

        Actions actions = new Actions(getDriver());
        actions.clickAndHold(redItem).moveToElement(canvas).release().perform();

        waitUntilDroppedItemsCount(1);

        WebElement droppedItem = findDroppedItems().get(0);

        String backgroundColor = droppedItem.getCssValue("background-color");
        // Red color in RGB format contains 255 for red and 0 for green/blue
        Assert.assertTrue(
                "Dropped item should have red background (got: "
                        + backgroundColor + ")",
                backgroundColor.contains("255") && backgroundColor.contains("0"));
    }

    private WebElement findDraggableByText(String text) {
        return findElement(By.xpath("//div[text()='" + text + "']"));
    }

    private WebElement findCanvas() {
        // Canvas has lightyellow background
        return findElement(By.xpath("//div[contains(@style, 'lightyellow')]"));
    }

    private List<WebElement> findDroppedItems() {
        WebElement canvas = findCanvas();
        return canvas
                .findElements(By.xpath(".//div[contains(text(), 'Item')]"));
    }

    private void waitUntilDroppedItemsCount(int count) {
        waitUntil(driver -> findDroppedItems().size() >= count, 10);
    }
}
