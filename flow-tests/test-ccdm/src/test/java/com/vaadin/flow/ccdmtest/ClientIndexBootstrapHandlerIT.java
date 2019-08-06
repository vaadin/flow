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

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientIndexBootstrapHandlerIT extends ChromeBrowserTest {

    private void openTestUrl(String url) {
        getDriver().get(getRootURL() + url);
    }

    @Test
    public void indexHtmlRequestHandler_openRootURL_shouldResponseIndexHtml() {
        openTestUrl("/");
        waitForElementPresent(By.tagName("div"));
        String content = findElement(By.id("content")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void indexHtmlRequestHandler_openRandomRoute_shouldResponseIndexHtml() {
        openTestUrl("/abc");
        waitForElementPresent(By.tagName("div"));
        String content = findElement(By.id("content")).getText();
        Assert.assertEquals("index.html content", content);
    }

    @Test
    public void indexHtmlRequestHandler_openURLHasParameterWithExtension_shouldResponseIndexHtml() {
        openTestUrl("/someroute?myparam=picture.png");
        waitForElementPresent(By.tagName("div"));
        String content = findElement(By.id("content")).getText();
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
        findElement(By.tagName("button")).click();
        WebElement contentFromJs = findElement(By.id("contentFromOtherBundle"));
        Assert.assertEquals("Content from other bundle",
                contentFromJs.getText());
    }

    @Test
    public void indexHtmlRequestHandler_openRootURL_shouldAddBaseHref() {
        openTestUrl("/");
        waitForElementPresent(By.tagName("div"));
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
        waitForElementPresent(By.tagName("div"));
        String outerHTML = findElement(By.tagName("head"))
                .findElement(By.tagName("base")).getAttribute("outerHTML");
        Assert.assertTrue(outerHTML.contains("href=\"./..\""));
    }
}
