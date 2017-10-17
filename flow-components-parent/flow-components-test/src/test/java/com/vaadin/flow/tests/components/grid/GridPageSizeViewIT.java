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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.tests.components.AbstractComponentIT;

public class GridPageSizeViewIT extends AbstractComponentIT {

    @Test
    public void gridWithPageSize10() {
        open();

        waitForElementPresent(By.tagName("vaadin-grid"));
        WebElement grid = findElement(By.tagName("vaadin-grid"));
        WebElement info = findElement(By.id("query-info"));

        Object pageSize = executeScript("return arguments[0].pageSize", grid);
        Assert.assertEquals("The pageSize of the webcomponent should be 10", 10,
                Integer.parseInt(String.valueOf(pageSize)));

        // the webcomponent use 3 * the page size as the query limit
        waitUntil(driver -> info.getText().endsWith("Query limit: 30"));
    }

}
