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
package com.vaadin.flow.ccdmtest;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientIndexHandlerIT extends ChromeBrowserTest {

    private void openTestUrl(String url) {
        getDriver().get(getRootURL() + url);
    }

    @Test
    public void indexHtmlRequestHandler_openRootURL_shouldResponseIndexHtml() {
        openTestUrl("/");
        waitForElementPresent(By.id("div0"));
        String content = findElement(By.id("div0")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void clientIndexBootstrapListener_openRootURL_shouldHaveModifiedLabel() {
        openTestUrl("/");
        waitForElementPresent(By.tagName("label"));
        String content = findElement(By.tagName("label")).getText();
        Assert.assertEquals(
                "The page should have label element which is added by a listener",
                "Modified page", content);
    }

    public void clientIndexBootstrapListener_openRootURL_shouldDynamicMetaContent() {
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
    public void indexHtmlRequestHandler_openURLWithExtension_shouldNotResponseIndexHtml() {
        openTestUrl("/not_found.png");
        String content = findElement(By.tagName("body")).getText();
        Assert.assertTrue(content.contains("404"));
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
        Assert.assertTrue("Flow.navigate should return view with parameter",
                content.contains("Parameter: " + inputParam));
        Assert.assertTrue("Flow.navigate should include router layout",
                content.contains("Main layout"));
    }

    @Test
    public void should_executeOnBeforeEnter_WhenViewIsBeforeEnterObserver() {
        openTestUrl("/");
        waitForElementPresent(By.id("button3"));
        findElement(By.id("pathname")).sendKeys("view-with-before-enter");
        findElement(By.id("button3")).click();
        waitForElementPresent(By.id("result"));

        String content = findElement(By.id("result")).getText();
        Assert.assertTrue("FlowJs should execute onBeforeEnter and navigate " +
                        "to ServerSideView",
                content.contains("Server view"));
        Assert.assertTrue("Flow.navigate should include router layout",
                content.contains("Main layout"));
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
}
