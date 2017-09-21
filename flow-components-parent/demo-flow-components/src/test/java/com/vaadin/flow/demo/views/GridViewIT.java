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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;

/**
 * Integration tests for the {@link GridView}.
 *
 */
public class GridViewIT extends AbstractChromeTest {

    @Before
    public void setUp() {
        open();
        waitForElementPresent(By.tagName("main-layout"));
    }

    @Test
    public void dataIsShown() {
        WebElement grid = findElement(By.id("basic"));
        WebElement header = grid
                .findElement(By.id("vaadin-grid-cell-content-0"));

        Assert.assertEquals("Name", header.getText());

        WebElement cell = grid.findElement(By.id("vaadin-grid-cell-content-2"));

        Assert.assertEquals("Person 1", cell.getText());
    }

    @Test
    public void lalzyDataIsShown() throws InterruptedException {
        WebElement grid = findElement(By.id("lazy-loading"));

        scrollToElement(grid);
        WebElement header = grid
                .findElement(By.tagName("vaadin-grid-cell-content"));

        Assert.assertEquals("Name", header.getText());

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            builder.append(
                    "arguments[0]._scrollPageDown();arguments[0]._scrollPageDown();");
        }
        getCommandExecutor().executeScript(builder.toString(), grid);

        WebElement cell = grid
                .findElements(By.tagName("vaadin-grid-cell-content")).get(2);

        waitUntil(driver -> "Person 1020".equals(cell.getText()));
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-grid";
    }
}
