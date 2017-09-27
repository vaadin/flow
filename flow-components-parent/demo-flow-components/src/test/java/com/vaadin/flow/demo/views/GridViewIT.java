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

import java.util.List;

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

        Assert.assertTrue(hasCell(grid, "Name"));

        Assert.assertTrue(hasCell(grid, "Person 1"));

        scroll(grid, 185);

        waitUntil(driver -> hasCell(grid, "Person 189"));
    }

    @Test
    public void lazyDataIsShown() throws InterruptedException {
        WebElement grid = findElement(By.id("lazy-loading"));

        scrollToElement(grid);

        Assert.assertTrue(hasCell(grid, "Name"));

        scroll(grid, 1010);

        Assert.assertTrue(hasCell(grid, "Person 1020"));
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
        scroll(grid, 495);
        waitUntilCellHasText(grid, "Person 499");
        // select item that is not in cache
        toggleButton.click();
        // scroll back up
        scroll(grid, 0);
        waitUntilCellHasText(grid, "Person 1");
        waitUntil(driver -> isRowSelected(grid, 0));
        Assert.assertEquals(
                getSelectionMessage(null, GridView.items.get(0), false),
                messageDiv.getText());
    }

    @Test
    public void gridWithDisabledSelection() {
        WebElement grid = findElement(By.id("none-selection"));
        scrollToElement(grid);
        grid.findElements(By.tagName("vaadin-grid-cell-content")).get(3)
                .click();
        Assert.assertFalse(isRowSelected(grid, 1));
    }

    private static String getSelectionMessage(Person oldSelection,
            Person newSelection, boolean isFromClient) {
        return String.format(
                "Selection changed from %s to %s, selection is from client: %s",
                oldSelection, newSelection, isFromClient);
    }

    private void scroll(WebElement grid, int index) {
        getCommandExecutor().executeScript(
                "arguments[0].scrollToIndex(" + index + ")", grid);
    }

    private void waitUntilCellHasText(WebElement grid, String text) {
        waitUntil(driver -> grid
                .findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .filter(cell -> text.equals(cell.getText())).findFirst()
                .isPresent());
    }

    private boolean isRowSelected(WebElement grid, int row) {
        return (boolean) getCommandExecutor().executeScript(
                "return arguments[0].shadowRoot.querySelectorAll('vaadin-grid-table-row')[arguments[1]].selected;",
                grid, row);
    }

    private boolean hasCell(WebElement grid, String text) {
        List<WebElement> cells = grid
                .findElements(By.tagName("vaadin-grid-cell-content"));
        return cells.stream().filter(cell -> text.equals(cell.getText()))
                .findAny().isPresent();
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-grid";
    }
}
