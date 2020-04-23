/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

@Category(IgnoreOSGi.class)
public class PushRouteNotFoundIT extends ChromeBrowserTest {

    @Test
    public void renderRouteNotFoundErrorPage_pushIsSpecifiedViaParentLayout() {
        open();

        TestBenchElement push = $(TestBenchElement.class).id("push-layout")
                .$(TestBenchElement.class).id("push-mode");
        Assert.assertEquals("Push mode: AUTOMATIC", push.getText());
    }

    @Test
    public void renderRouteNotFoundErrorPage_parentLayoutReroute_reroutingIsDone() {
        String url = getTestURL(getRootURL(),
                doGetTestPath(PushLayout.FORWARD_PATH), new String[0]);

        getDriver().get(url);

        waitUntil(driver -> driver.getCurrentUrl()
                .endsWith(ForwardPage.class.getName()));

        Assert.assertTrue(isElementPresent(By.id("forwarded")));
    }

    @Override
    protected String getTestPath() {
        return doGetTestPath(PushRouteNotFoundView.PUSH_NON_EXISTENT_PATH);
    }

    private String doGetTestPath(String uri) {
        String path = super.getTestPath();
        int index = path.lastIndexOf("/");
        return path.substring(0, index + 1) + uri;
    }
}
