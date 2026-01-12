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
package com.vaadin.flow.uitest.ui.routing;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(SlowTests.class)
public class InfiniteRerouteLoopIT extends ChromeBrowserTest {

    private static final String NAVIGATION_EXCEPTION = "navigation-exception";

    @Test
    public void renderNavigationExceptionTarget_locationIsNotChanged() {
        open();

        waitUntil(driver -> isElementPresent(By.tagName("body")));

        Assert.assertTrue(
                driver.getCurrentUrl().endsWith(NAVIGATION_EXCEPTION));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        int index = path.lastIndexOf("/");
        return path.substring(0, index + 1) + NAVIGATION_EXCEPTION;
    }

    @Override
    protected Class<? extends Component> getViewClass() {
        return PushRouteNotFoundView.class;
    }

}
