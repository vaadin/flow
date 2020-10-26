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
package com.vaadin.flow.ccdmtest;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

public class IndexHtmlRequestHandlerIT extends CCDMTest {

    @Test
    public void indexHtmlRequestHandler_openRootURL_shouldResponseIndexHtml() {
        openTestUrl("/");
        waitForElementPresent(By.id("div0"));
        String content = findElement(By.id("div0")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void indexHtmlRequestListener_openRootURL_shouldHaveModifiedLabel() {
        openTestUrl("/");
        waitForElementPresent(By.tagName("label"));
        String content = findElement(By.tagName("label")).getText();
        Assert.assertEquals(
                "The page should have label element which is added by a listener",
                "Modified page", content);
    }

    @Test
    public void should_add_appShellAnnotations() {
        openTestUrl("/");
        waitForElementPresent(By.tagName("meta"));
        WebElement meta = findElement(By.cssSelector("meta[name=foo]"));
        Assert.assertNotNull(meta);
        Assert.assertEquals("bar", meta.getAttribute("content"));
        WebElement metaViewPort = findElement(
                By.cssSelector("meta[name=viewport]"));
        Assert.assertNotNull(metaViewPort);
        Assert.assertEquals(
                "width=device-width, height=device-height, initial-scale=1.0",
                metaViewPort.getAttribute("content"));
    }

    @Test
    public void should_accessVaadinService_getCurrent_inAppShellConstructor() {
        openTestUrl("/");
        waitForElementPresent(By.tagName("meta"));
        WebElement meta = findElement(
                By.cssSelector("meta[name=test-resource-url]"));
        Assert.assertNotNull(meta);
        Assert.assertEquals("my-resource", meta.getAttribute("content"));
    }

    public void indexHtmlRequestListener_openRootURL_shouldDynamicMetaContent() {
        openTestUrl("/");
        waitForElementPresent(By.cssSelector("meta[name]"));
        Optional<WebElement> ogImageMeta = findElements(By.tagName("meta"))
                .stream().filter(webElement -> webElement.getAttribute("name")
                        .equals("og:image"))
                .findFirst();
        Assert.assertTrue("The response should have ogImage meta element",
                ogImageMeta.isPresent());
        Assert.assertEquals(
                "ogImage meta element should have correct image URL",
                "http://localhost:8888/image/my_app.png",
                ogImageMeta.get().getAttribute("content"));

        Optional<WebElement> viewportMeta = findElements(By.tagName("meta"))
                .stream().filter(webElement -> webElement.getAttribute("name")
                        .equals("viewport"))
                .findFirst();
        Assert.assertTrue("The response should have viewport meta element",
                viewportMeta.isPresent());
        Assert.assertEquals("viewport meta element should have correct content",
                "width=device-width, height=device-height, initial-scale=1.0",
                viewportMeta.get().getAttribute("content"));
    }

    public void indexHtmlRequestListener_openRootURL_shouldDynamicBodySizeContent() {
        openTestUrl("/");
        waitForElementPresent(By.cssSelector("style"));
        Optional<WebElement> styleElement = findElements(By.tagName("style"))
                .stream().filter(webElement -> webElement.getAttribute("type")
                        .equals("text/css"))
                .findFirst();
        Assert.assertTrue("The response should have style element",
                styleElement.isPresent());
        Assert.assertEquals("style element should have correct content",
                "body,#outlet{height:50vh;width:50vw;}",
                styleElement.get().getText());
    }

    @Test
    public void indexHtmlRequestHandler_openRandomRoute_shouldResponseIndexHtml() {
        openTestUrl("/abc");
        waitForElementPresent(By.id("div0"));
        String content = findElement(By.id("div0")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void indexHtmlRequestHandler_openURLHasParameterWithExtension_shouldResponseIndexHtml() {
        openTestUrl("/someroute?myparam=picture.png");
        waitForElementPresent(By.id("div0"));
        String content = findElement(By.id("div0")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void indexHtmlRequestHandler_should_bootstrapAnyRoute() {
        openTestUrl("/com.example.foo.Bar");
        String content = findElement(By.id("div0")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void indexHtmlRequestHandler_importDynamically_shouldLoadBundleCorrectly() {
        openTestUrl("/");
        waitForElementPresent(By.id("button1"));

        findElement(By.id("button1")).click();
        waitForElementPresent(By.id("div1"));

        String content = findElement(By.id("div1")).getText();
        Assert.assertEquals("Content from other bundle", content);
    }

    @Test
    public void indexHtmlRequestHandler_openRootURL_shouldAddBaseHref() {
        openTestUrl("/");
        waitForElementPresent(By.id("div0"));
        // In Selenium, getAttribute('href') won't return the exact value of
        // 'href'.
        // https://stackoverflow.com/questions/35494519/how-to-get-the-exact-text-of-href-attribute-of-tag-a
        String outerHTML = findElement(By.tagName("head"))
                .findElement(By.tagName("base")).getAttribute("outerHTML");
        Assert.assertTrue(outerHTML.contains("href=\".\""));
    }

    @Test
    public void indexHtmlRequestHandler_openTwoSlashesURL_shouldAddBaseHrefCorrectly() {
        openTestUrl("/abc/xyz");
        waitForElementPresent(By.id("div0"));
        String outerHTML = findElement(By.tagName("head"))
                .findElement(By.tagName("base")).getAttribute("outerHTML");
        Assert.assertTrue(outerHTML.contains("href=\"./..\""));
    }

    @Test
    public void should_startFlow() {
        openTestUrl("/");
        waitForElementPresent(By.id("button2"));

        findElement(By.id("button2")).click();
        waitForElementPresent(By.id("div2"));

        String content = findElement(By.id("div2")).getText();
        Assert.assertEquals("true true true", content);
    }

    @Test
    public void should_connectFlowServerSide() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));

        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertTrue(content.contains("Empty view"));
    }

    @Test
    public void should_bootstrapFlowCorrectly_When_CallingFromMultipleSegmentsPath() {
        openTestUrl("/foo/bar/far");
        waitForElementPresent(By.id("button3"));
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertTrue(content.contains("Empty view"));
    }

    @Test
    public void should_removeFirstSlash_whenRouteStartsWithSlash() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));

        findElement(By.id("pathname")).sendKeys("/");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertTrue("It should ignore the first slash in route path",
                content.contains("Empty view"));
    }

    @Test
    public void should_getViewByRoute_WhenNavigate() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));

        findElement(By.id("pathname")).sendKeys("serverview");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertTrue("Flow.navigate should return view by route from "
                + "server views", content.contains("Server view"));

        Assert.assertTrue("Flow.navigate should include router layout",
                content.contains("Main layout"));
    }

    @Test
    public void should_executeSetParameter_WhenRouteHasParameter() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        String inputParam = "123";
        findElement(By.id("pathname")).sendKeys("paramview/" + inputParam);
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertEquals("Should execute set parameter method",
                "Main layout\nParameter: " + inputParam, content);
    }

    @Test
    public void should_executeSetParameter_WhenRouteHasParameterAndQueryString() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        String inputParam = "123";
        String queryString = "query1=123&query2=456";
        findElement(By.id("pathname"))
                .sendKeys("paramview/" + inputParam + "?" + queryString);
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertEquals("Should execute set parameter method",
                "Main layout\nParameter: " + inputParam + " - Query string: "
                        + queryString,
                content);
    }

    @Test
    public void should_rerouteToOtherView_WhenViewRerouteInOnBeforeEnter() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        findElement(By.id("pathname")).sendKeys("reroute-with-before-enter");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertEquals(
                "Should execute onBeforeEnter and reroute to ServerSideView",
                "Main layout\nServer view", content);
    }

    @Test
    public void should_forwardToOtherView_WhenViewForwardInOnBeforeEnter() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        findElement(By.id("pathname")).sendKeys("forward-with-before-enter");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertEquals(
                "Should execute onBeforeEnter and forward to ServerSideView",
                "Main layout\nServer view", content);
    }

    @Test
    public void should_executeNavigationEventsInCorrectOrder_When_navigateToServerView() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        findElement(By.id("pathname")).sendKeys("view-with-all-events");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();

        String expectedContent = "Main layout\nViewWithAllEvents: 1 "
                + "setParameter\nViewWithAllEvents: 2 "
                + "beforeEnter\nViewWithAllEvents: 3 "
                + "onAttach\nViewWithAllEvents: 4 afterNavigation";
        Assert.assertEquals("Should execute all lifecycle callbacks",
                expectedContent, content);
    }

    @Test
    public void should_preserveView_When_reloadPreservedOnRefreshView() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        findElement(By.id("pathname")).sendKeys("preserve");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String shouldBePreservedText = "should be preserved";
        findElement(By.id("inputPreserved")).sendKeys(shouldBePreservedText);
        // trigger on change event in the text field to sync to server
        findElement(By.id("inputPreserved")).sendKeys(("\t"));
        // refresh and reload flow to create a new UI
        getDriver().navigate().refresh();
        waitForElementPresent(By.id("button3"));
        findElement(By.id("pathname")).sendKeys("preserve");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String inputPreservedContent = findElement(By.id("inputPreserved"))
                .getAttribute("value");
        Assert.assertEquals("Text in the input field should be preserved",
                shouldBePreservedText, inputPreservedContent);
    }

    @Test
    public void should_navigateFromClientToServer_When_UsingVaadinRouter() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("Client view")).click();
        waitForElementPresent(By.id("clientView"));
        String clientSideViewContent = findElement(By.id("clientView"))
                .getText();
        Assert.assertEquals(
                "Should load client side view content with vaadin-router",
                "Client view", clientSideViewContent);

        findElement(By.linkText("Server view")).click();
        waitForElementPresent(By.id("mainLayout"));
        String mainLayoutContent = findElement(By.id("mainLayout")).getText();
        Assert.assertEquals(
                "Should load server view content with " + "vaadin-router",
                "Main layout\nServer view", mainLayoutContent);

    }

    @Test
    public void should_navigateFromServerToClient_When_UsingVaadinRouter() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("Server view")).click();
        waitForElementPresent(By.id("mainLayout"));
        String mainLayoutContent = findElement(By.id("mainLayout")).getText();
        Assert.assertEquals(
                "Should load server view content with " + "vaadin-router",
                "Main layout\nServer view", mainLayoutContent);

        findElement(By.linkText("Client view")).click();
        waitForElementPresent(By.id("clientView"));
        String clientSideViewContent = findElement(By.id("clientView"))
                .getText();
        Assert.assertEquals(
                "Should load client side view content with vaadin-router",
                "Client view", clientSideViewContent);
    }

    @Test
    public void should_navigateFromServerToServer_When_UsingVaadinRouter() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("View with all events")).click();
        waitForElementPresent(By.id("mainLayout"));
        String mainLayoutContent = findElement(By.id("mainLayout")).getText();
        String expectedContent = "Main layout\nViewWithAllEvents: 1 "
                + "setParameter\nViewWithAllEvents: 2 "
                + "beforeEnter\nViewWithAllEvents: 3 "
                + "onAttach\nViewWithAllEvents: 4 afterNavigation";
        Assert.assertEquals("Should load view with all events", expectedContent,
                mainLayoutContent);

        findElement(By.linkText("Server view")).click();
        waitForElementPresent(By.id("serverView"));
        String serverViewContent = findElement(By.id("mainLayout")).getText();
        Assert.assertEquals(
                "Should load server side view content with vaadin-router",
                "Main layout\nServer view", serverViewContent);
    }

    @Test
    public void should_returnNotFoundView_WhenRouteNotFound() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));

        findElement(By.id("pathname")).sendKeys("not-existing-view");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertTrue("Flow.navigate should return not found view",
                content.contains("Could not navigate"));
    }

    @Test
    public void should_notRunExtraEvents_When_NavigatingServerClientServer() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("View with all events")).click();
        waitForElementPresent(By.id("mainLayout"));
        String mainLayoutContent = findElement(By.id("mainLayout")).getText();
        String expectedContent = "Main layout\nViewWithAllEvents: 1 "
                + "setParameter\nViewWithAllEvents: 2 "
                + "beforeEnter\nViewWithAllEvents: 3 "
                + "onAttach\nViewWithAllEvents: 4 afterNavigation";
        Assert.assertEquals("Should load view with all events", expectedContent,
                mainLayoutContent);

        findElement(By.linkText("Client view")).click();
        waitForElementPresent(By.id("clientView"));
        String clientSideViewContent = findElement(By.id("clientView"))
                .getText();
        Assert.assertEquals(
                "Should load client side view content with vaadin-router",
                "Client view", clientSideViewContent);

        findElement(By.linkText("View with all events")).click();
        waitForElementPresent(By.id("mainLayout"));
        mainLayoutContent = findElement(By.id("mainLayout")).getText();
        expectedContent = "Main layout\nViewWithAllEvents: 1 "
                + "setParameter\nViewWithAllEvents: 2 "
                + "beforeEnter\nViewWithAllEvents: 3 "
                + "onAttach\nViewWithAllEvents: 4 afterNavigation";
        Assert.assertEquals("Should load a fresh view with all events",
                expectedContent, mainLayoutContent);
    }

    @Test
    public void should_renderServerView_When_ClickSameServerLinkTwiceAndGoToClientView() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("View with all events")).click();
        waitForElementPresent(By.id("mainLayout"));
        // click the link again to trigger `action` of `flow.route`
        findElement(By.linkText("View with all events")).click();
        String mainLayoutContent = findElement(By.id("mainLayout")).getText();
        String expectedContent = "Main layout\n"
                + "ViewWithAllEvents: 1 setParameter\n"
                + "ViewWithAllEvents: 2 beforeEnter\n"
                + "ViewWithAllEvents: 3 onAttach\n"
                + "ViewWithAllEvents: 4 afterNavigation\n"
                + "ViewWithAllEvents: beforeLeave\n"
                + "ViewWithAllEvents: 1 setParameter\n"
                + "ViewWithAllEvents: 2 beforeEnter\n"
                + "ViewWithAllEvents: 4 afterNavigation";

        Assert.assertEquals("Should load view with all events", expectedContent,
                mainLayoutContent);

        findElement(By.linkText("Client view")).click();
        waitForElementPresent(By.id("clientView"));
        String clientSideViewContent = findElement(By.id("clientView"))
                .getText();
        Assert.assertEquals(
                "Should load client side view content with vaadin-router",
                "Client view", clientSideViewContent);

        findElement(By.linkText("View with all events")).click();
        waitForElementPresent(By.id("mainLayout"));
        mainLayoutContent = findElement(By.id("mainLayout")).getText();
        expectedContent = "Main layout\nViewWithAllEvents: 1 "
                + "setParameter\nViewWithAllEvents: 2 "
                + "beforeEnter\nViewWithAllEvents: 3 "
                + "onAttach\nViewWithAllEvents: 4 afterNavigation";
        Assert.assertEquals("Should load a fresh view with all events",
                expectedContent, mainLayoutContent);
    }

    @Test
    public void should_passCorrectLocationObject_When_NavigateServerToClient() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("Prevent leaving view")).click();
        waitForElementPresent(By.id("mainLayout"));
        findElement(By.id("preventRouteInput")).sendKeys("client-view\t");
        findElement(By.id("preventRouteButton")).click();
        waitForElementPresent(By.cssSelector("p[class=\"prevented-route\"]"));

        findElement(By.linkText("Client view")).click();
        String preventedRoute = findElement(
                By.cssSelector("p[class" + "=\"prevented-route\"]")).getText();
        Assert.assertEquals(
                "Should pass correct location object in "
                        + "navigation events so that the view can prevent the "
                        + "navigation and stay in the same view",
                "preventing navigation to 'client-view'", preventedRoute);

        findElement(By.linkText("Server view")).click();
        waitForElementPresent(By.id("serverView"));
        String mainLayoutContent = findElement(By.id("mainLayout")).getText();
        Assert.assertEquals("Should be able to navigate to other view",
                "Main" + " layout\nServer view", mainLayoutContent);
    }

    @Test
    public void should_goToHomeView_When_ServerNavigatesToEmptyPath() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("View with home button")).click();
        waitForElementPresent(By.id("viewWithHomeButton"));
        findElement(By.id("homeButton")).click();

        waitForElementPresent(By.id("emptyUi"));
        String homeViewContent = findElement(By.id("emptyUi")).getText();
        Assert.assertEquals(
                "Should render home view when calling ui.navigate(\"\")",
                "Empty view", homeViewContent);

        String expectedUrl = getRootURL() + "/foo/";
        String actualUrl = getDriver().getCurrentUrl();
        Assert.assertEquals("Should update URL to context path when calling"
                + " ui.navigate(\"\"),", expectedUrl, actualUrl);
    }

    @Test
    public void should_updateBrowserUrl_When_ServerNavigates() {
        openTestUrl("/");
        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));

        findElement(By.linkText("View with server view button")).click();
        waitForElementPresent(By.id("viewWithServerViewButton"));
        findElement(By.id("serverViewButton")).click();

        waitForElementPresent(By.id("serverView"));
        String expectedUrl = getRootURL() + "/foo/serverview";
        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertEquals(
                "Should update the browser url when calling"
                        + " ui.navigate(\"serverview\")",
                expectedUrl, currentUrl);
    }

    @Test
    public void should_loadPushScript_When_FlowStarts() {
        openTestUrl("/");
        waitForElementPresent(By.id("button2"));
        findElement(By.id("button2")).click();
        waitForElementPresent(By.id("div2"));

        Assert.assertEquals("Should have window.vaadinPush", "object",
                executeScript("return typeof window.vaadinPush"));
        Assert.assertEquals("Should have window.vaadinPush.atmosphere",
                "object", executeScript(
                        "return typeof window.vaadinPush" + ".atmosphere"));
    }

    @Test
    public void should_receiveServerPushUpdates() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));

        findElement(By.id("pathname")).sendKeys("/push");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("pushedContent"));

        String content = findElement(By.id("pushedContent")).getText();
        Assert.assertEquals("Should have content sent from server using push mechanism",
                "Message pushed from server", content);
    }

    @Test
    @Ignore // until serviceworker installation works in test hub browser
    public void should_installServiceWorker() {
        openTestUrl("/");
        boolean serviceWorkerActive = (boolean) ((JavascriptExecutor) getDriver())
                .executeAsyncScript("const resolve = arguments[arguments.length - 1];"
                        + "navigator.serviceWorker.ready.then( function(reg) { resolve(!!reg.active); });");
        Assert.assertTrue("service worker not installed", serviceWorkerActive);
    }
}
