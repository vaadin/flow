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
package com.vaadin.flow.tests.components.grid;

import static org.junit.Assert.assertFalse;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;

public class GridItemRefreshViewIT extends AbstractComponentIT {

    @Test
    public void updateAndRefreshItemsOnTheServer() {
        open();

        WebElement grid = findElement(By.tagName("vaadin-grid"));
        WebElement refreshFirstItem = findElement(By.id("refresh-first"));
        WebElement refreshMultipleItems = findElement(
                By.id("refresh-multiple"));
        WebElement refreshAll = findElement(By.id("refresh-all"));

        assertNotUpdated(grid, 0, 0);
        refreshFirstItem.click();
        waitUntilUpdated(grid, 0, 0);

        assertNotUpdated(grid, 4, 9);
        refreshMultipleItems.click();
        waitUntilUpdated(grid, 4, 9);

        assertNotUpdated(grid, 10, 15);
        refreshAll.click();
        waitUntilUpdated(grid, 10, 15);

        getCommandExecutor().executeScript("arguments[0].scrollToIndex(900)",
                grid);
        // rows at the bottom (outside of the initial cache) should also be
        // updated
        waitUntilUpdated(grid, 900, 910);
    }

    private void waitUntilUpdated(WebElement grid, int startIndex,
            int lastIndex) {
        Set<String> expected = IntStream.range(startIndex, lastIndex + 1)
                .mapToObj(intVal -> "updated " + String.valueOf(intVal))
                .collect(Collectors.toSet());
        waitUntil(driver -> grid
                .findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .map(cell -> cell.getText()).collect(Collectors.toSet())
                .containsAll(expected));
    }

    private void assertNotUpdated(WebElement grid, int startIndex,
            int lastIndex) {
        Set<String> expected = IntStream.range(startIndex, lastIndex + 1)
                .mapToObj(intVal -> "updated " + String.valueOf(intVal))
                .collect(Collectors.toSet());
        assertFalse(grid
                .findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .map(cell -> cell.getText()).collect(Collectors.toSet())
                .removeAll(expected));
    }
}
