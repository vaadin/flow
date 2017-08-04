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
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.demo.AbstractChromeTest;
import com.vaadin.testbench.By;

/**
 * Integration tests for the {@link FormLayoutView}.
 */
public class FormLayoutIT extends AbstractChromeTest {

    private WebElement layout;

    @Before
    public void init() {
        open();
        waitForElementPresent(By.tagName("main-layout"));
        layout = findElement(By.tagName("main-layout"));
        Assert.assertTrue(isElementPresent(By.tagName("vaadin-form-layout")));
    }

    @Test
    public void custom_responsive_layouting() {
        WebElement firstLayout = layout
                .findElement(By.tagName("vaadin-form-layout"));
        List<WebElement> textFields = firstLayout
                .findElements(By.tagName("vaadin-text-field"));
        Assert.assertEquals(3, textFields.size());

        getDriver().manage().window().setSize(new Dimension(1000, 1000));

        // 3 columns, all should be horizontally aligned (tolerance of 2 pixels
        // given)
        Assert.assertTrue("All 3 columns should be horizontally aligned",
                Math.abs(textFields.get(2).getLocation().getY()
                - textFields.get(1).getLocation().getY()) < 2);
        Assert.assertTrue(Math.abs(textFields.get(1).getLocation().getY()
                - textFields.get(0).getLocation().getY()) < 2);

        getDriver().manage().window().setSize(new Dimension(450, 620));

        // window resized, should be in 2 column mode, last textfield below
        // other two
        Assert.assertTrue(
                "Layout should be in 2 column mode, last field should be below the first two",
                textFields.get(2).getLocation().getY() > textFields
                .get(1).getLocation().getY());
        Assert.assertTrue(textFields.get(2).getLocation().getY() > textFields
                .get(0).getLocation().getY());

        getDriver().manage().window().setSize(new Dimension(400, 620));

        // resized to 1 column mode, fields should be arranged below one another
        Assert.assertTrue(
                "Layout should be in 1 column mode, all fields should be below one another",
                textFields.get(2).getLocation().getY() > textFields
                .get(1).getLocation().getY());
        Assert.assertTrue(textFields.get(1).getLocation().getY() > textFields
                .get(0).getLocation().getY());
    }

    @Override
    protected String getTestPath() {
        return "/vaadin-form-layout";
    }
}
