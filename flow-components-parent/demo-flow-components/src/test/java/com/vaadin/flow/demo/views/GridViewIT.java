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
package com.vaadin.flow.demo.views;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
import com.vaadin.flow.demo.views.GridView.Person;
import com.vaadin.testbench.By;

/**
 * Integration tests for the {@link GridView}.
 *
 */
public class GridViewIT extends ComponentDemoTest {

    @Test
    public void dataIsShown() throws InterruptedException {
        WebElement grid = findElement(By.id("basic"));
        WebElement header = grid
                .findElement(By.id("vaadin-grid-cell-content-0"));

        Assert.assertEquals("Name", header.getText());

        WebElement cell1 = grid
                .findElement(By.id("vaadin-grid-cell-content-2"));

        Assert.assertEquals("Person 1", cell1.getText());

        scrollDown(grid, 9);

        waitUntil(driver -> grid
                .findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .filter(cell -> "Person 189".equals(cell.getText())).findFirst()
                .isPresent());
    }

    @Test
    public void lalzyDataIsShown() throws InterruptedException {
        WebElement grid = findElement(By.id("lazy-loading"));

        scrollToElement(grid);
        WebElement header = grid
                .findElement(By.tagName("vaadin-grid-cell-content"));

        Assert.assertEquals("Name", header.getText());

        scrollDown(grid, 50);

        WebElement cell = grid
                .findElements(By.tagName("vaadin-grid-cell-content")).get(2);

        waitUntil(driver -> "Person 1020".equals(cell.getText()));
    }

    @Test
    public void gridAsSingleSelect() {
        WebElement grid = findElement(By.id("single-selection"));
        scrollToElement(grid);

        WebElement toggleButton = findElement(By.id("single-selection-toggle"));
        WebElement messageDiv = findElement(By.id("single-selection-message"));

        toggleButton.click();
        Assert.assertEquals(
                getSelectionMessage(null, GridView.items.get(0), false),
                messageDiv.getText());
        Assert.assertTrue(isRowSelected(grid, 0));
        toggleButton.click();
        Assert.assertEquals(
                getSelectionMessage(GridView.items.get(0), null, false),
                messageDiv.getText());
        Assert.assertFalse(isRowSelected(grid, 0));

        // should be the cell in the first column's second row
        grid.findElement(By.id("vaadin-grid-cell-content-111")).click();
        Assert.assertTrue(isRowSelected(grid, 1));
        Assert.assertEquals(
                getSelectionMessage(null, GridView.items.get(1), true),
                messageDiv.getText());
        toggleButton.click();
        Assert.assertTrue(isRowSelected(grid, 0));
        Assert.assertFalse(isRowSelected(grid, 1));
        Assert.assertEquals(getSelectionMessage(GridView.items.get(1),
                GridView.items.get(0), false), messageDiv.getText());
        toggleButton.click();
        Assert.assertFalse(isRowSelected(grid, 0));

        // scroll to bottom
        scrollDown(grid, 50);
        // select item that is not in cache
        toggleButton.click();
        // scroll back up
        scrollUp(grid, 50);
        Assert.assertEquals(
                getSelectionMessage(null, GridView.items.get(0), false),
                messageDiv.getText());
        Assert.assertTrue(isRowSelected(grid, 0));
    }

    private static String getSelectionMessage(Person oldSelection,
            Person newSelection, boolean isFromClient) {
        return String.format(
                "Selection changed from %s to %s, selection is from client: %s",
                oldSelection, newSelection, isFromClient);
    }

    private void scrollDown(WebElement grid, int pageNumbers) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pageNumbers; i++) {
            builder.append(
                    "arguments[0]._scrollPageDown();arguments[0]._scrollPageDown();");
        }
        getCommandExecutor().executeScript(builder.toString(), grid);
    }

    private void scrollUp(WebElement grid, int pageNumbers) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pageNumbers; i++) {
            builder.append(
                    "arguments[0]._scrollPageUp();arguments[0]._scrollPageUp();");
        }
        getCommandExecutor().executeScript(builder.toString(), grid);
    }

    private boolean isRowSelected(WebElement grid, int row) {
        return (boolean) getCommandExecutor().executeScript(
                "return arguments[0].shadowRoot.querySelectorAll('vaadin-grid-table-row')[arguments[1]].selected;",
                grid, row);
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-grid";
    }
}
