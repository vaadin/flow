/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NavigationIT extends ChromeBrowserTest {
    @Test
    public void testNavigation() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @Test
    public void testNavigationPostpone_anchor() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.POSTPONE_ID).click();

        Assert.assertEquals("PostponeView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assert.assertEquals("Navigation should have postponed", "PostponeView",
                $(SpanElement.class).first().getText());

        Assert.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assert.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assert.assertTrue($(NativeButtonElement.class).id(PostponeView.STAY_ID)
                .isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.STAY_ID).click();

        Assert.assertEquals(0, $(NativeButtonElement.class).all().size());

        Assert.assertTrue("Url should not have changed",
                getDriver().getCurrentUrl().endsWith("PostponeView"));

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assert.assertEquals(
                "Navigation should have fired and be postponed again",
                "PostponeView", $(SpanElement.class).first().getText());

        Assert.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assert.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assert.assertTrue($(NativeButtonElement.class).id(PostponeView.STAY_ID)
                .isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.CONTINUE_ID).click();

        Assert.assertEquals(
                "Navigation should have continued to NavigationView",
                "NavigationView", $(SpanElement.class).first().getText());

        Assert.assertTrue(
                "Url should have updated to NavigationView was : "
                        + getDriver().getCurrentUrl(),
                getDriver().getCurrentUrl().endsWith("NavigationView"));
    }

    @Test
    public void testNavigationPostpone_routerLink() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.POSTPONE_ID).click();

        Assert.assertEquals("PostponeView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ROUTER_LINK_ID)
                .click();

        Assert.assertEquals("Navigation should have postponed", "PostponeView",
                $(SpanElement.class).first().getText());

        Assert.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assert.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assert.assertTrue($(NativeButtonElement.class).id(PostponeView.STAY_ID)
                .isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.STAY_ID).click();

        Assert.assertEquals(0, $(NativeButtonElement.class).all().size());

        Assert.assertTrue("Url should not have changed",
                getDriver().getCurrentUrl().endsWith("PostponeView"));

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ROUTER_LINK_ID)
                .click();

        Assert.assertEquals(
                "Navigation should have fired and be postponed again",
                "PostponeView", $(SpanElement.class).first().getText());

        Assert.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assert.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assert.assertTrue($(NativeButtonElement.class).id(PostponeView.STAY_ID)
                .isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.CONTINUE_ID).click();

        Assert.assertEquals(
                "Navigation should have continued to NavigationView",
                "NavigationView", $(SpanElement.class).first().getText());

        Assert.assertTrue(
                "Url should have updated to NavigationView was : "
                        + getDriver().getCurrentUrl(),
                getDriver().getCurrentUrl().endsWith("NavigationView"));
    }

    @Test
    public void testNavigationBrowserHistoryBack_anchor() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @Test
    public void testNavigationBrowserHistoryBack_routerLink() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @Test
    public void testNavigationBrowserHistoryBack_serverNavigation() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @Test
    public void testRouterLinkWithQueryNavigation() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_QUERY_ID).click();
        Assert.assertEquals(
                "Exception on router-link navigation with query parameters",
                "AnchorView", $(SpanElement.class).first().getText());
        System.out.println(getDriver().getCurrentUrl());
        Assert.assertTrue("Query was missing in url",
                getDriver().getCurrentUrl().endsWith("?test=value"));
    }

    @Test
    public void testAnchorWithQueryNavigation() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_QUERY_ID).click();
        Assert.assertEquals(
                "Exception on router-link navigation with query parameters",
                "AnchorView", $(SpanElement.class).first().getText());
        System.out.println(getDriver().getCurrentUrl());
        Assert.assertTrue("Query was missing in url",
                getDriver().getCurrentUrl().endsWith("?test=anchor"));
    }

    private void checkNavigatedEvent(String log) {
        Object message = ((JavascriptExecutor) getDriver())
                .executeScript("return window.testMessage;");

        Assert.assertTrue(message instanceof String);
        Assert.assertEquals(log, message);
    }

    @Test
    public void testNavigatedEvent() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        // When entering the page, vaadin-navigated event is not fired
        checkNavigatedEvent(
                "navigated to /view/com.vaadin.flow.NavigationView");

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.AnchorView");
        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent(
                "navigated to /view/com.vaadin.flow.NavigationView");

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.ServerView");
        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent(
                "navigated to /view/com.vaadin.flow.NavigationView");

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.RouterView");
        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent(
                "navigated to /view/com.vaadin.flow.NavigationView");
    }

    @Test
    public void testNavigatedForPostponeView() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.POSTPONE_ID).click();

        Assert.assertEquals("PostponeView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.PostponeView");

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assert.assertEquals("Navigation should have postponed", "PostponeView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.PostponeView");

        Assert.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assert.assertTrue($(NativeButtonElement.class).id(PostponeView.STAY_ID)
                .isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.STAY_ID).click();

        Assert.assertEquals(0, $(NativeButtonElement.class).all().size());

        Assert.assertTrue("Url should not have changed",
                getDriver().getCurrentUrl().endsWith("PostponeView"));
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.PostponeView");

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assert.assertEquals(
                "Navigation should have fired and be postponed again",
                "PostponeView", $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /view/com.vaadin.flow.PostponeView");

        Assert.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assert.assertTrue($(NativeButtonElement.class).id(PostponeView.STAY_ID)
                .isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.CONTINUE_ID).click();

        Assert.assertEquals(
                "Navigation should have continued to NavigationView",
                "NavigationView", $(SpanElement.class).first().getText());
        checkNavigatedEvent(
                "navigated to /view/com.vaadin.flow.NavigationView");
    }

    @Test
    public void testNavigation_HasUrlParameter_setParameterCalledOnce() {
        open();

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id(NavigationView.SET_PARAMETER_COUNTER_ID).getText());
    }
}
