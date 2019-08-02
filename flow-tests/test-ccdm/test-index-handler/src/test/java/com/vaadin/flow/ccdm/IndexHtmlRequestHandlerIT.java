package com.vaadin.flow.ccdm;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class IndexHtmlRequestHandlerIT extends ChromeBrowserTest {

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
    public void indexHtmlRequestHandler_openURLHasParameterWithExtension_shouldNotResponseIndexHtml() {
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
    public void indexHtmlRequestHandler_hasLazilyLoadedBundle_shouldLazyBundleCorrectly() {
        openTestUrl("/");
        findElement(By.tagName("button")).click();
        waitForElementPresent(By.tagName("vaadin-button"));
        WebElement vaadinButton = findElement(By.tagName("vaadin-button"));
        Assert.assertEquals("Vaadin Button", vaadinButton.getText());
    }

    @Test
    public void indexHtmlRequestHandler_openRootURL_shouldHasBaseHref() {
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
    public void indexHtmlRequestHandler_openTwoSlashesURL_shouldHasBaseHrefCorrectly() {
        openTestUrl("/abc/xyz");
        waitForElementPresent(By.tagName("div"));
        String outerHTML = findElement(By.tagName("head"))
                .findElement(By.tagName("base")).getAttribute("outerHTML");
        Assert.assertTrue(outerHTML.contains("href=\"./..\""));
    }
}
