package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class RouterLinkIT extends PhantomJSTest {

    @Test
    public void testRoutingLinks_insideServletMapping_updateLocation() {
        open();

        verifySamePage();

        testInsideServlet("foo", "foo");
        testInsideServlet("./foobar", "foobar");
        testInsideServlet("foo/bar", "foo/bar");

        testInsideServlet("./foobar?what=not", "foobar?what=not");

        testInsideServlet("/run/baz", "baz");

        testInsideServlet("./foobar?what=not#fragment", "foobar?what=not",
                "foobar?what=not#fragment");

        clickLink("empty");
        verifyInsideServletLocation("");
        verifySamePage();
    }

    @Test
    public void testRoutingLinks_outsideServletMapping_pageChanges() {
        open();

        verifySamePage();

        clickLink("/run");
        verifyNotSamePage();
    }

    @Test
    public void testRoutingLinks_externalLink_pageChanges() {
        open();

        verifySamePage();

        clickLink("http://google.com");

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue("Invalid URL: " + currentUrl,
                currentUrl.startsWith("http://www.google."));
    }

    @Test
    public void testImageInsideRouterLink() {
        open();

        verifySamePage();

        findElement(By.tagName("img")).click();

        verifyInsideServletLocation("image/link");
        verifyPopStateEvent("image/link");
    }

    private void testInsideServlet(String linkToTest,
            String pathAfterServletMapping) {
        testInsideServlet(linkToTest, pathAfterServletMapping,
                pathAfterServletMapping);
    }

    private void testInsideServlet(String linkToTest, String popStateLocation,
            String pathAfterServletMapping) {
        clickLink(linkToTest);
        verifyInsideServletLocation(pathAfterServletMapping);
        verifyPopStateEvent(popStateLocation);
        verifySamePage();
    }

    private void clickLink(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    private void verifyInsideServletLocation(String pathAfterServletMapping) {
        Assert.assertEquals("Invalid URL",
                getRootURL() + "/run/" + pathAfterServletMapping,
                getDriver().getCurrentUrl());
    }

    private void verifyPopStateEvent(String location) {
        Assert.assertEquals("Invalid server side event location", location,
                findElement(By.id("location")).getText());
    }

    private void verifyNotSamePage() {
        Assert.assertEquals(0, findElements(By.id("location")).size());
    }

    private void verifySamePage() {
        Assert.assertNotNull("Page has changed",
                findElement(By.id("location")));
    }
}
