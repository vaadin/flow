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
        testInsideServlet("foo/bar", "foo/bar");

        testInsideServlet("./foobar", "foobar");
        testInsideServlet("./foobar?what=not", "foobar?what=not");
        testInsideServlet("./foobar?what=not#fragment", "foobar?what=not");

        testInsideServlet("/run/baz", "baz");

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

    private void testInsideServlet(String linkToTest,
            String pathAfterServletMapping) {
        clickLink(linkToTest);
        verifyInsideServletLocation(pathAfterServletMapping);
        verifyPopStateEvent(pathAfterServletMapping);
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
