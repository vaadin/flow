/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.navigate;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.navigate.HelloWorldView.NAVIGATE_ABOUT;

public class NavigateBetweenViewsIT extends ChromeBrowserTest {

    @Test
    public void openFlowView_navigateToTsView_navigationSuccessful() {
        getDriver().get(getRootURL() + "/hello");
        waitForDevServer();

        Assert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/hello"));

        $(NativeButtonElement.class).id(NAVIGATE_ABOUT).click();

        // Wait for component inside shadowroot as there is no vaadin
        // to wait for as with server-side
        waitUntil(input -> $("about-view").first().$("a").id("navigate-hello")
                .isDisplayed());

        Assert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/about"));
        Assert.assertTrue(getInShadowRoot(findElement(By.tagName("about-view")),
                By.id("navigate-hello")).isDisplayed());
    }

    @Test
    public void openTsView_navigateToFlow_navigationSuccessful() {
        getDriver().get(getRootURL() + "/");
        waitForDevServer();

        // Wait for component inside shadowroot as there is no vaadin
        // to wait for as with server-side
        waitUntil(input -> $("about-view").first().$("a").id("navigate-hello")
                .isDisplayed());

        getInShadowRoot(findElement(By.tagName("about-view")),
                By.id("navigate-hello")).click();

        Assert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/hello"));

        Assert.assertTrue("Missing expected native button on page",
                $(NativeButtonElement.class).id(NAVIGATE_ABOUT).isDisplayed());
    }

}
