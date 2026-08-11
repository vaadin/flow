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
package com.vaadin.flow.test.routing;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(NavigationView.class)
public class NavigationIT extends AbstractDefaultIT {
    @BrowserTest
    public void testNavigation() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assertions.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assertions.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @BrowserTest
    public void testNavigationPostpone_anchor() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.POSTPONE_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText(),
                "Navigation should have postponed");

        Assertions.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.STAY_ID).isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.STAY_ID).click();

        Assertions.assertEquals(0, $(NativeButtonElement.class).all().size());

        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("PostponeView"),
                "Url should not have changed");

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText(),
                "Navigation should have fired and be postponed again");

        Assertions.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.STAY_ID).isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.CONTINUE_ID).click();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText(),
                "Navigation should have continued to NavigationView");

        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("NavigationView"),
                "Url should have updated to NavigationView was : "
                        + getDriver().getCurrentUrl());
    }

    @BrowserTest
    public void testNavigationPostpone_routerLink() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.POSTPONE_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ROUTER_LINK_ID)
                .click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText(),
                "Navigation should have postponed");

        Assertions.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.STAY_ID).isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.STAY_ID).click();

        Assertions.assertEquals(0, $(NativeButtonElement.class).all().size());

        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("PostponeView"),
                "Url should not have changed");

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ROUTER_LINK_ID)
                .click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText(),
                "Navigation should have fired and be postponed again");

        Assertions.assertEquals(2, $(NativeButtonElement.class).all().size());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.STAY_ID).isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.CONTINUE_ID).click();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText(),
                "Navigation should have continued to NavigationView");

        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("NavigationView"),
                "Url should have updated to NavigationView was : "
                        + getDriver().getCurrentUrl());
    }

    @BrowserTest
    public void testNavigationBrowserHistoryBack_anchor() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assertions.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assertions.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @BrowserTest
    public void testNavigationBrowserHistoryBack_routerLink() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assertions.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assertions.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @BrowserTest
    public void testNavigationBrowserHistoryBack_serverNavigation() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @BrowserTest
    public void testreactNavigationBrowserHistoryBack_anchor() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.REACT_ANCHOR_ID).click();
        Assertions.assertEquals("This is a simple view for a React route",
                $(ParagraphElement.class).id("react").getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.REACT_ANCHOR_ID).click();
        Assertions.assertEquals("This is a simple view for a React route",
                $(ParagraphElement.class).id("react").getText());
        getDriver().navigate().back();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

    @BrowserTest
    public void testReactNavigation_flowContentCleaned() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.REACT_ANCHOR_ID).click();
        Assertions.assertEquals("This is a simple view for a React route",
                $(ParagraphElement.class).id("react").getText());

        Assertions.assertFalse($(AnchorElement.class)
                .attribute("id", NavigationView.REACT_ANCHOR_ID).exists(),
                "Flow navigation view contents should not exist");
    }

    @BrowserTest
    public void testReactNavigationBrowserHistoryBack_serverNavigation() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.REACT_ID).click();
        waitUntil(driver -> "This is a simple view for a React route"
                .equals($(ParagraphElement.class).id("react").getText()));
        getDriver().navigate().back();
        waitUntil(driver -> "NavigationView"
                .equals($(SpanElement.class).first().getText()));

        $(NativeButtonElement.class).id(NavigationView.REACT_ID).click();
        waitUntil(driver -> "This is a simple view for a React route"
                .equals($(ParagraphElement.class).id("react").getText()));
        getDriver().navigate().back();
        waitUntil(driver -> "NavigationView"
                .equals($(SpanElement.class).first().getText()));
    }

    @BrowserTest
    public void testRouterLinkWithQueryNavigation() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_QUERY_ID).click();
        Assertions.assertEquals("AnchorView",
                $(SpanElement.class).first().getText(),
                "Exception on router-link navigation with query parameters");
        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("?test=value"),
                "Query was missing in url");
    }

    @BrowserTest
    public void testAnchorWithQueryNavigation() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_QUERY_ID).click();
        Assertions.assertEquals("AnchorView",
                $(SpanElement.class).first().getText(),
                "Exception on router-link navigation with query parameters");
        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("?test=anchor"),
                "Query was missing in url");
    }

    private void checkNavigatedEvent(String log) {
        Object message = ((JavascriptExecutor) getDriver())
                .executeScript("return window.testMessage;");

        Assertions.assertTrue(message instanceof String);
        Assertions.assertEquals(log, message);
    }

    @BrowserTest
    public void testNavigatedEvent() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.NavigationView");

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assertions.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.AnchorView");
        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.NavigationView");

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.ServerView");
        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.NavigationView");

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assertions.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.RouterView");
        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.NavigationView");
    }

    @BrowserTest
    public void testNavigatedForPostponeView() {
        open();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.POSTPONE_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText());
        checkNavigatedEvent("navigated to /com.vaadin.flow.PostponeView");

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText(),
                "Navigation should have postponed");
        checkNavigatedEvent("navigated to /com.vaadin.flow.PostponeView");

        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.STAY_ID).isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.STAY_ID).click();

        Assertions.assertEquals(0, $(NativeButtonElement.class).all().size());

        Assertions.assertTrue(
                getDriver().getCurrentUrl().endsWith("PostponeView"),
                "Url should not have changed");
        checkNavigatedEvent("navigated to /com.vaadin.flow.PostponeView");

        $(AnchorElement.class).id(PostponeView.NAVIGATION_ID).click();

        Assertions.assertEquals("PostponeView",
                $(SpanElement.class).first().getText(),
                "Navigation should have fired and be postponed again");
        checkNavigatedEvent("navigated to /com.vaadin.flow.PostponeView");

        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.CONTINUE_ID).isDisplayed());
        Assertions.assertTrue($(NativeButtonElement.class)
                .id(PostponeView.STAY_ID).isDisplayed());

        $(NativeButtonElement.class).id(PostponeView.CONTINUE_ID).click();

        Assertions.assertEquals("NavigationView",
                $(SpanElement.class).first().getText(),
                "Navigation should have continued to NavigationView");
        checkNavigatedEvent("navigated to /com.vaadin.flow.NavigationView");
    }

    @BrowserTest
    public void testNavigation_HasUrlParameter_setParameterCalledOnce() {
        open();

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assertions.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        Assertions.assertEquals("1", $(SpanElement.class)
                .id(NavigationView.SET_PARAMETER_COUNTER_ID).getText());
    }
}
