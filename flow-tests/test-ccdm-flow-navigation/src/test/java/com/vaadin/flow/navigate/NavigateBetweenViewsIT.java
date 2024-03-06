/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.navigate;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.navigate.HelloWorldView.IS_CONNECTED_ON_ATTACH;
import static com.vaadin.flow.navigate.HelloWorldView.IS_CONNECTED_ON_INIT;
import static com.vaadin.flow.navigate.HelloWorldView.NAVIGATE_ABOUT;

public class NavigateBetweenViewsIT extends ChromeBrowserTest {

    private static final String PARAM_VALUE_SPECIAL_CHARACTERS = "a=b%20  `'¨åäö";
    private static final String ROUTE_SPECIAL_CHARACTERS = "special åäö $%20'´`";

    @Test
    public void openFlowView_navigateToTsView_navigationSuccessful() {
        getDriver().get(getRootURL() + "/hello");
        waitForDevServer();

        Assert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/hello"));

        waitUntil(driver -> $(NativeButtonElement.class).id(NAVIGATE_ABOUT))
                .click();

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

    @Test
    public void openViewWithSpecialCharactersInRoute() {
        getDriver().get(getRootURL() + "/"
                + UrlUtil.encodeURI(ROUTE_SPECIAL_CHARACTERS));
        waitForDevServer();
        assertSpecialViewShown();
    }

    @Test
    public void openViewWithSpecialCharactersInQueryParameters() {
        getDriver().get(getRootURL() + "/hello?value="
                + UrlUtil.encodeURIComponent(PARAM_VALUE_SPECIAL_CHARACTERS));
        waitForDevServer();
        assertHelloViewShown();
        Assert.assertEquals("value: " + PARAM_VALUE_SPECIAL_CHARACTERS,
                $("*").id("params").getText());
    }

    @Test
    public void navigateToViewWithSpecialCharactersInRoute() {
        getDriver().get(getRootURL() + "/hello");
        waitForDevServer();
        waitUntil(input -> {
            TestBenchElement view = $("*").id("hello-world-view");
            TestBenchElement link = view.$("a").id("navigate-special");
            return link;
        }).click();

        assertSpecialViewShown();
    }

    @Test
    public void navigateFromViewWithSpecialCharactersInRoute() {
        getDriver().get(getRootURL() + "/"
                + UrlUtil.encodeURI(ROUTE_SPECIAL_CHARACTERS));
        waitForDevServer();

        waitUntil(input -> {
            TestBenchElement view = $("*").id("special-view");
            TestBenchElement link = view.$("a").id("navigate-hello");
            return link;
        }).click();
        assertHelloViewShown();
    }

    private void assertSpecialViewShown() {
        TestBenchElement specialView = waitUntil(driver -> {
            return $("*").id("special-view");
        });
        Assert.assertEquals("This is the special view",
                specialView.$("*").id("title").getText());
    }

    private void assertHelloViewShown() {
        TestBenchElement helloView = waitUntil(driver -> {
            return $("*").id("hello-world-view");
        });
        Assert.assertEquals("Say hello",
                helloView.$("*").id("navigate-about").getText());
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
