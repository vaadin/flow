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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.ComponentDemoTest;
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
        getCell(grid, "Person 2").click();
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
    public void gridAsMultiSelect() {
        WebElement grid = findElement(By.id("multi-selection"));
        scrollToElement(grid);

        WebElement selectBtn = findElement(By.id("multi-selection-button"));
        WebElement messageDiv = findElement(By.id("multi-selection-message"));

        selectBtn.click();
        Assert.assertEquals(
                getSelectionMessage(Collections.emptySet(),
                        GridView.items.subList(0, 5), false),
                messageDiv.getText());
        assertRowsSelected(grid, 0, 5);

        List<WebElement> checkboxes = grid
                .findElements(By.tagName("vaadin-checkbox")).stream()
                .filter(element -> "Select Row"
                        .equals(element.getAttribute("aria-label")))
                .collect(Collectors.toList());
        clickCheckbox(checkboxes.get(0));
        clickCheckbox(checkboxes.get(1));
        Assert.assertEquals(
                getSelectionMessage(GridView.items.subList(1, 5),
                        GridView.items.subList(2, 5), true),
                messageDiv.getText());
        assertRowsSelected(grid, 2, 5);

        clickCheckbox(checkboxes.get(5));
        Assert.assertTrue(isRowSelected(grid, 5));
        selectBtn.click();
        assertRowsSelected(grid, 0, 5);
        Assert.assertFalse(isRowSelected(grid, 5));
    }

    @Test
    public void gridWithDisabledSelection() {
        WebElement grid = findElement(By.id("none-selection"));
        scrollToElement(grid);
        grid.findElements(By.tagName("vaadin-grid-cell-content")).get(3)
                .click();
        Assert.assertFalse(isRowSelected(grid, 1));
    }

    @Test
    public void gridWithColumnTemplate() {
        WebElement grid = findElement(By.id("template-renderer"));
        scrollToElement(grid);
        Assert.assertTrue(hasHtmlCell(grid, "0"));
        Assert.assertTrue(hasHtmlCell(grid,
                "<div title=\"Person 1\">Person 1<br><small>23 years old</small></div>"));
        Assert.assertTrue(hasHtmlCell(grid,
                "<div>Street S, number 30<br><small>16142</small></div>"));

        WebElement buttonsCell = getHtmlCell(grid,
                "<button>Update</button><button>Remove</button>");
        List<WebElement> buttons = buttonsCell
                .findElements(By.tagName("button"));
        Assert.assertEquals(2, buttons.size());

        clickElementWithJs(buttons.get(0));
        waitUntil(driver -> hasHtmlCell(grid,
                "<div title=\"Person 1 Updated\">Person 1 Updated<br><small>23 years old</small></div>"));

        clickElementWithJs(buttons.get(0));
        waitUntil(driver -> hasHtmlCell(grid,
                "<div title=\"Person 1 Updated Updated\">Person 1 Updated Updated<br><small>23 years old</small></div>"));

        clickElementWithJs(buttons.get(1));
        waitUntilNot(driver -> hasHtmlCell(grid,
                "<div title=\"Person 1 Updated Updated\">Person 1 Updated Updated<br><small>23 years old</small></div>"));
    }

    @Test
    public void gridColumnApiTests() {
        WebElement grid = findElement(By.id("column-api-example"));
        scrollToElement(grid);

        Assert.assertEquals("Two resize handlers should be present", 2L,
                getCommandExecutor().executeScript(
                        "return arguments[0].shadowRoot.querySelectorAll('[part~=\"resize-handle\"]').length;",
                        grid));

        Assert.assertEquals("First width is fixed", "75px",
                getCommandExecutor().executeScript(
                        "return arguments[0].shadowRoot.querySelectorAll('th')[0].style.width;",
                        grid));

        WebElement toggleIdColumnVisibility = findElement(
                By.id("toggle-id-column-visibility"));
        String firstCellHiddenScript = "return arguments[0].shadowRoot.querySelectorAll('td')[0].hidden;";
        Assert.assertNotEquals(true, getCommandExecutor()
                .executeScript(firstCellHiddenScript, grid));
        toggleIdColumnVisibility.click();
        Assert.assertEquals(true, getCommandExecutor()
                .executeScript(firstCellHiddenScript, grid));
        toggleIdColumnVisibility.click();
        Assert.assertNotEquals(true, getCommandExecutor()
                .executeScript(firstCellHiddenScript, grid));

        Assert.assertNotEquals("true",
                grid.getAttribute("columnReorderingAllowed"));

        WebElement toggleUserReordering = findElement(
                By.id("toggle-user-reordering"));
        toggleUserReordering.click();
        Assert.assertEquals("true",
                grid.getAttribute("columnReorderingAllowed"));
        toggleUserReordering.click();
        Assert.assertNotEquals("true",
                grid.getAttribute("columnReorderingAllowed"));

        String idColumnFrozenStatusScript = "return arguments[0].shadowRoot.querySelectorAll('th')[0].getAttribute('frozen')";
        WebElement toggleIdColumnFrozen = findElement(
                By.id("toggle-id-column-frozen"));
        Assert.assertEquals(null, getCommandExecutor()
                .executeScript(idColumnFrozenStatusScript, grid));
        toggleIdColumnFrozen.click();
        Assert.assertEquals("", getCommandExecutor()
                .executeScript(idColumnFrozenStatusScript, grid));
        toggleIdColumnFrozen.click();
        Assert.assertEquals(null, getCommandExecutor()
                .executeScript(idColumnFrozenStatusScript, grid));
    }

    private static String getSelectionMessage(Object oldSelection,
            Object newSelection, boolean isFromClient) {
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

    private void assertRowsSelected(WebElement grid, int first, int last) {
        IntStream.range(first, last).forEach(
                rowIndex -> Assert.assertTrue(isRowSelected(grid, rowIndex)));
    }

    private boolean isRowSelected(WebElement grid, int row) {
        WebElement gridRow = getInShadowRoot(grid, By.id("items"))
                .findElements(By.cssSelector("tr")).get(row);
        return gridRow.getAttribute("selected") != null;
    }

    private boolean hasCell(WebElement grid, String text) {
        return getCell(grid, text) != null;
    }

    private WebElement getCell(WebElement grid, String text) {
        List<WebElement> cells = grid
                .findElements(By.tagName("vaadin-grid-cell-content"));
        return cells.stream().filter(cell -> text.equals(cell.getText()))
                .findAny().orElse(null);
    }

    private boolean hasHtmlCell(WebElement grid, String html) {
        return getHtmlCell(grid, html) != null;
    }

    private WebElement getHtmlCell(WebElement grid, String text) {
        List<WebElement> cells = grid
                .findElements(By.tagName("vaadin-grid-cell-content"));
        return cells.stream()
                .filter(cell -> text.equals(cell.getAttribute("innerHTML")))
                .findAny().orElse(null);
    }

    private void clickCheckbox(WebElement checkbox) {
        getInShadowRoot(checkbox, By.id("nativeCheckbox")).click();
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-grid";
    }
}
