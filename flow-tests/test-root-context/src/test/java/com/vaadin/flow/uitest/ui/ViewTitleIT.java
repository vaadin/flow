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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.html.testbench.SelectElement;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class ViewTitleIT extends ChromeBrowserTest {

    @Override
    protected void open() {
        getDriver().get(getRootURL() + "/view/");
        waitForDevServer();
    }

    @Test
    public void testNoViewTitle() {
        open();
        openView("BasicElementView");

        verifyTitle("");
    }

    @Test
    public void testSetTitleAfterNavigationEvent() {
        open();
        openView("SetTitleAfterNavigationEventView");

        verifyTitle("my-changed-title-after-AfterNavigationEvent");
    }

    @Test
    public void testViewTitleAnnotation() {
        open();
        openView("TitleView");

        verifyTitle("Title view");
    }

    @Test
    public void testViewDynamicTitle() {
        open();
        openView("DynamicTitleView");

        verifyTitle("dynamic title view");
    }

    private void openView(String viewName) {
        SelectElement input = $(SelectElement.class).first();
        input.selectByText(viewName);
    }

    private void verifyTitle(String title) {
        Assert.assertEquals("Page title does not match", title,
                getDriver().getTitle());
    }

}
