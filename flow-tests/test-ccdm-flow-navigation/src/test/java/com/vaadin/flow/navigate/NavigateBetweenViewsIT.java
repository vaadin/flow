/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.navigate.HelloWorldView.IS_CONNECTED_ON_ATTACH;
import static com.vaadin.flow.navigate.HelloWorldView.IS_CONNECTED_ON_INIT;
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
        TestBenchElement aboutView = $("about-view").first();

        Assert.assertTrue(aboutView.$("*").id("navigate-hello").isDisplayed());
    }

    @Test
    public void openTsView_navigateToFlow_navigationSuccessful() {
        getDriver().get(getRootURL() + "/");
        waitForDevServer();

        // Wait for component inside shadowroot as there is no vaadin
        // to wait for as with server-side
        waitUntil(input -> $("about-view").first().$("a").id("navigate-hello")
                .isDisplayed());

        TestBenchElement aboutView = $("about-view").first();
        aboutView.$("*").id("navigate-hello").click();

        getCommandExecutor().waitForVaadin();

        Assert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/hello"));

        Assert.assertTrue("Missing expected native button on page",
                $(NativeButtonElement.class).id(NAVIGATE_ABOUT).isDisplayed());
    }

    @Test
    public void openFlowView_isConnectedOnAttach() {
        getDriver().get(getRootURL() + "/hello");
        waitForDevServer();

        Assert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/hello"));

        assertIsConnected();

        // Navigate away and back
        $(NativeButtonElement.class).id(NAVIGATE_ABOUT).click();
        waitUntil(input -> $("about-view").first().$("a").id("navigate-hello")
                .isDisplayed());

        TestBenchElement aboutView = $("about-view").first();

        aboutView.$("*").id("navigate-hello").click();

        assertIsConnected();
    }

    @Test
    public void openTsView_navigateToFlowView_isConnectedOnAttach() {
        getDriver().get(getRootURL() + "/");
        waitForDevServer();

        waitUntil(input -> $("about-view").first().$("a").id("navigate-hello")
                .isDisplayed());

        TestBenchElement aboutView = $("about-view").first();
        aboutView.$("*").id("navigate-hello").click();

        getCommandExecutor().waitForVaadin();

        assertIsConnected();
    }

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context-path";
    }

    private void assertIsConnected() {
        assertIsConnectedById(IS_CONNECTED_ON_INIT);
        assertIsConnectedById(IS_CONNECTED_ON_ATTACH);
    }

    private void assertIsConnectedById(String id) {
        Assert.assertTrue(Boolean.parseBoolean(
                waitUntil(driver -> $(SpanElement.class).id(id)).getText()));
    }
}
