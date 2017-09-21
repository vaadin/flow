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

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;

public class GridViewIT extends AbstractComponentIT {

    @Test
    public void basicGrid() {
        open();

        WebElement header1 = findElement(By.id("vaadin-grid-cell-content-0"));
        Assert.assertEquals("text", header1.getText());

        Optional<WebElement> header2 = findElements(
                By.tagName("vaadin-grid-cell-content")).stream()
                .filter(cell -> "length".equals(cell.getText())).findFirst();
        Assert.assertTrue(header2.isPresent());

        String id = header2.get().getAttribute("id");
        int indx = id.lastIndexOf("-");
        int index = Integer.parseInt(id.substring(indx + 1, id.length()));

        WebElement column1 = findElement(By.id("vaadin-grid-cell-content-2"));
        Assert.assertEquals("0", column1.getText());

        WebElement column2 = findElement(
                By.id("vaadin-grid-cell-content-" + (index + 2)));
        Assert.assertEquals("1", column2.getText());

        // Scroll 50 pages down and check that data is available
        WebElement grid = findElement(By.tagName("vaadin-grid"));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            builder.append(
                    "arguments[0]._scrollPageDown();arguments[0]._scrollPageDown();");
        }
        getCommandExecutor().executeScript(
                builder.toString(),
                grid);

        waitUntil(driver -> "1050".equals(column1.getText()));
        Assert.assertEquals("4", column2.getText());

        // change data provider
        findElement(By.id("update-provider")).click();

        waitUntil(driver -> hasData());
    }

    private boolean hasData() {
        Set<String> data = new HashSet<>();
        data.add("foo");
        data.add("foob");
        data.add("fooba");
        data.add("foobar");
        Collection<String> lengths = data.stream().map(String::length)
                .map(Object::toString)
                .collect(Collectors.toList());
        data.addAll(lengths);
        findElements(By.tagName("vaadin-grid-cell-content"))
                .forEach(cell -> data.remove(cell.getText()));
        return data.isEmpty();
    }
}
