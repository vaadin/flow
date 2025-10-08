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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet.NAVIGATE_BACK_FROM_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID;
import static com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet.NAVIGATE_BACK_FROM_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID;
import static com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet.NAVIGATE_TO_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID;
import static com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet.NAVIGATE_TO_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID;
import static com.vaadin.flow.uitest.servlet.RouterLayoutCustomScopeServlet.SUB_LAYOUT_ID;

public class RemoveRoutersLayoutContentIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/router-layout-custom-scope/first-child-route";
    }

    @Test
    public void removeUIScopedRouterLayoutContent_navigateToAnotherRouteInsideMainLayoutAndBack_subLayoutOldContentRemoved() {
        open();
        navigate(NAVIGATE_TO_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID);
        waitForElementPresent(By.id(
                NAVIGATE_BACK_FROM_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID));
        navigate(
                NAVIGATE_BACK_FROM_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID);
        waitForElementPresent(By.id(SUB_LAYOUT_ID));

        assertSubLayoutHasNoOldContent();
    }

    @Test
    public void removeUIScopedRouterLayoutContent_navigateToAnotherRouteOutsideMainLayoutAndBack_mainLayoutOldContentRemoved() {
        open();
        navigate(NAVIGATE_TO_ANOTHER_ROUTE_OUTSIDE_MAIN_LAYOUT_BUTTON_ID);
        waitForElementPresent(By.id(
                NAVIGATE_BACK_FROM_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID));
        navigate(NAVIGATE_BACK_FROM_ANOTHER_ROUTE_INSIDE_MAIN_LAYOUT_BUTTON_ID);
        waitForElementPresent(By.id(SUB_LAYOUT_ID));

        assertSubLayoutHasNoOldContent();
    }

    private void assertSubLayoutHasNoOldContent() {
        TestBenchElement subLayout = $("div").id(SUB_LAYOUT_ID);
        List<WebElement> subLayoutChildren = subLayout
                .findElements(By.tagName("div"));
        Assert.assertEquals(1, subLayoutChildren.size());
    }

    private void navigate(String navigateButtonId) {
        $("button").id(navigateButtonId).click();
    }
}
