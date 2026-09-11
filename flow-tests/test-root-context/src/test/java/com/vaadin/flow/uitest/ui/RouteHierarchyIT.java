/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.SelectElement;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class RouteHierarchyIT extends ChromeBrowserTest {

    @Test
    public void breadcrumbBuiltFromInstanceFreeRouteHierarchy() {
        getDriver().get(getRootURL() + "/view/");
        waitForDevServer();
        $(SelectElement.class).first().selectByText("RouteHierarchyView");

        // The breadcrumb is built from RouteConfiguration#getRouteHierarchy:
        // the @RouteParent ancestor (Orders) and the current view, with each
        // title resolved without instantiating the ancestor route.
        String breadcrumb = $(DivElement.class).id("breadcrumb").getText();
        Assert.assertTrue(
                "Breadcrumb should contain the parent and current titles, was: "
                        + breadcrumb,
                breadcrumb.contains("Orders / Order details"));

        // the current view title is applied from @PageTitle during navigation
        Assert.assertEquals("Order details", getDriver().getTitle());
    }
}
